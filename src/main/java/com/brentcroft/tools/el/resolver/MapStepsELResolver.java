package com.brentcroft.tools.el.resolver;

import com.brentcroft.tools.el.Evaluator;
import com.brentcroft.tools.el.ReturnException;
import com.brentcroft.tools.el.StepsException;
import com.brentcroft.tools.el.TextExpander;
import com.brentcroft.tools.el.MapBindings;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Stack;

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
                                     : null;

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

        if ( steps.isEmpty()) {
            context.setPropertyResolved( base, methodName );
            return null;
        }

        int[] lineNumber = { 0 };
        String[] lastStep = { null };

        // indicates it's a local scope
        // and can receive and encapsulate assignments
        MapBindings localScope = new MapBindings()
                .withEntry( "$local", null )
                .withEntry( "$functionName", stepsKey );

        scopeStack.get().push(localScope);

        try
        {
            MapBindings scope = newContainer( root );

            if ( args != null )
            {
                scope.putAll( args );
            }

            Object[] lastResult = { null };

            Evaluator
                    .stepsStream( steps )
                    .peek( step -> {
                        lineNumber[ 0 ]++;
                        lastStep[ 0 ] = step;
                    } )
                    .map( step -> expander.expandText( step, scope ) )
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
}
