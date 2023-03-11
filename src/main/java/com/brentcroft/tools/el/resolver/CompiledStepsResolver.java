package com.brentcroft.tools.el.resolver;

import com.brentcroft.tools.el.ReturnException;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ValueExpression;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Stack;

import static java.lang.String.format;

@AllArgsConstructor
public class CompiledStepsResolver extends BaseELResolver
{
    private final ThreadLocal< Stack< Map< String, Object > > > scopeStack;

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

        String stepsKey = format( "$$$%s", methodName );

        if ( ! root.containsKey( stepsKey ) )
        {
            return null;
        }

        if ( params != null && params.length > 0 && ! ( params[ 0 ] instanceof Map ) )
        {
            throw new IllegalArgumentException( "Compiled Steps call must have one argument that is a map, or no argument at all" );
        }

        Map< String, Object > scope = newContainer( root );

        // indicates it's a local scope
        // and can receive assignments
        scope.put( "$local", null );

        ValueExpression steps = ( ValueExpression ) root.get( stepsKey );

        scope.put( "$functionName", stepsKey );

        if ( args != null )
        {
            scope.putAll( args );
        }

        scopeStack.get().push( scope );

        try
        {
            Object ret = steps.getValue( context );
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

            throw e;
        }
        finally
        {
            scopeStack.get().pop();
        }
    }
}
