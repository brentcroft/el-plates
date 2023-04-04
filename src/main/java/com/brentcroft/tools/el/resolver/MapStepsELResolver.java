package com.brentcroft.tools.el.resolver;

import com.brentcroft.tools.el.*;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

@AllArgsConstructor
public class MapStepsELResolver extends BaseELResolver
{
    private final ThreadLocal< Stack< MapBindings > > scopeStack;
    private final Evaluator evaluator;
    private final TextExpander expander;

    @SuppressWarnings( "unchecked" )
    public Object invoke( ELContext context, Object base, Object methodName, Class< ? >[] paramTypes, Object[] params )
    {
        if ( base == null || methodName == null )
        {
            return null;
        }
        if ( ! ( base instanceof Map ) )
        {
            return null;
        }

        @SuppressWarnings( "unchecked" )
        Map< String, Object > root = ( Map< String, Object > ) base;
        Map< String, Object > args = ( params != null && params.length > 0 && params[ 0 ] instanceof Map )
                                     ? ( Map< String, Object > ) params[ 0 ]
                                     : new HashMap<>();

        String stepsKey = format( "$$%s", methodName );

        if ( ! root.containsKey( stepsKey ) )
        {
            return null;
        }

        if ( params != null && params.length > 0 && ! ( params[ 0 ] instanceof Map ) )
        {
            throw new IllegalArgumentException( "Steps call must have one argument that is a map, or no argument at all" );
        }

        String steps = format( "%s", root.get( stepsKey ) ).trim();

        if ( steps.isEmpty() )
        {
            context.setPropertyResolved( base, methodName );
            return null;
        }

        String stepsArgsKey = format( "$$%s$args", methodName );
        if ( root.containsKey( stepsArgsKey ) )
        {
            checkStepsArgs( args, root.get( stepsArgsKey ) );
        }


        final String logStepsKey = "$logSteps";
        boolean logSteps = root.containsKey( logStepsKey )
                && Boolean.parseBoolean( root.get( logStepsKey ).toString() );

        int[] lineNumber = { 0 };
        String[] lastStep = { null };

        // indicates it's a local scope
        // and can receive and encapsulate assignments
        MapBindings localScope = new MapBindings()
                .withEntry( "$local", null )
                .withEntry( "$functionName", stepsKey );

        scopeStack.get().push( localScope );

        try
        {
            MapBindings scope = newContainer( root );

            if ( args != null )
            {
                scope.putAll( args );
            }

            Object[] lastResult = { null };

            String indent = logSteps
                            ? IntStream
                                    .range( 0, scopeStack.get().size() )
                                    .mapToObj( i -> "  " )
                                    .collect( Collectors.joining() )
                            : "";

            if ( logSteps )
            {
                System.out.printf( "%s%s (steps)%n", indent, stepsKey );
            }

            Evaluator
                    .stepsStream( steps )
                    .peek( step -> {
                        lineNumber[ 0 ]++;
                        lastStep[ 0 ] = step;
                    } )
                    .map( step -> expander.expandText( step, scope ) )
                    .peek( step -> {
                        if ( logSteps )
                        {
                            System.out.printf( "%s[%d] -> %s%n", indent, lineNumber[ 0 ], step );
                        }
                    } )
                    .forEachOrdered( step -> lastResult[ 0 ] = evaluator.eval( step, scope ) );
            Object ret = lastResult[ 0 ];
            context.setPropertyResolved( base, methodName );
            return ret;

        }
        catch ( ReturnException e )
        {
            context.setPropertyResolved( base, methodName );
            return e.get();

        }
        catch ( RuntimeException e )
        {
            RuntimeException cause = e;
            while ( cause.getCause() != null && cause.getCause() instanceof ELException )
            {
                cause = ( ELException ) cause.getCause();
            }
            if ( cause instanceof ReturnException )
            {
                context.setPropertyResolved( base, methodName );
                return ( ( ReturnException ) cause ).get();
            }

            throw new StepsException( lineNumber[ 0 ], lastStep[ 0 ], base, methodName, cause );
        }
        finally
        {
            scopeStack.get().pop();
        }
    }

    private void checkStepsArgs( Map< String, Object > args, Object expectedArgs )
    {
        if ( ! ( expectedArgs instanceof Map ) )
        {
            if ( expectedArgs != null )
            {
                throw new IllegalArgumentException( format(
                        "Expected args is not a Map: class=%s, value=%s ",
                        expectedArgs.getClass().getSimpleName(),
                        expectedArgs ) );
            }
            return;
        }
        @SuppressWarnings( "unchecked" )
        Map< String, Object > ea = ( Map< String, Object > ) expectedArgs;
        ea.forEach( ( k, v ) -> {
            if ( ! args.containsKey( k ) )
            {
                if (v == null) {
                    throw new IllegalArgumentException( format( "Missing mandatory arg: %s ", k ) );
                }
            }
            if ( v instanceof Class )
            {
                Class< ? > c = ( Class< ? > ) v;
                if ( ! args.containsKey( k ) )
                {
                    throw new IllegalArgumentException( format( "Missing mandatory arg: %s of type: %s", k, c.getSimpleName() ) );
                }
                Object value = args.get( k );
                if ( ! c.isInstance(value ) )
                {
                    throw new IllegalArgumentException( format(
                            "Invalid arg: %s, expected type %s, received type: %s ",
                            k,
                            c.getSimpleName(),
                            value == null ? null : value.getClass().getSimpleName()
                    ) );
                }
            } else {
                if ( ! args.containsKey( k ) )
                {
                    // default value
                    args.put( k, v );
                    return;
                }

                Class< ? > c = v.getClass();
                Object value = args.get( k );

                if ( ! c.isInstance( value ) )
                {
                    throw new IllegalArgumentException( format(
                            "Invalid arg: %s, expected type %s, received type: %s ",
                            k,
                            c.getSimpleName(),
                            value == null ? null : value.getClass().getSimpleName()
                    ) );
                }
            }
        } );
    }
}
