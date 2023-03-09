package com.brentcroft.tools.el.resolver;

import com.brentcroft.tools.el.Evaluator;
import com.brentcroft.tools.el.ReturnException;
import com.brentcroft.tools.el.TextExpander;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import lombok.AllArgsConstructor;

import java.util.Map;

import static java.lang.String.format;

@AllArgsConstructor
public class MapStepsELResolver extends BaseELResolver
{
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

        Map< String, Object > scope = newContainer( root );

        String steps = format( "%s", root.get( stepsKey ) );

        scope.put( "$functionName", stepsKey );

        if ( args != null )
        {
            scope.putAll( args );
        }

        int[] lineNumber = { 0 };
        String[] lastStep = { null };

        try
        {
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

            throw new ELException( format( "Failed at step [%s] %s; base=%s, method=%s", lineNumber[ 0 ], lastStep[0], base, methodName ), cause );
        }
    }
}
