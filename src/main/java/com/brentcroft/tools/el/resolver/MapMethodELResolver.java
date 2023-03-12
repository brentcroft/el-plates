package com.brentcroft.tools.el.resolver;

import jakarta.el.ELContext;
import jakarta.el.LambdaExpression;
import lombok.AllArgsConstructor;

import java.util.Map;

import static java.lang.String.format;

@AllArgsConstructor
public class MapMethodELResolver extends BaseELResolver
{
    public Object invoke( ELContext context, Object base, Object methodName, Class< ? >[] paramTypes, Object[] params )
    {
        if ( ! ( base instanceof Map ) || methodName == null )
        {
            return null;
        }

        @SuppressWarnings( "unchecked" )
        Map< String, Object > root = ( Map< String, Object > ) base;

        String runnableKey = format( "%s", methodName );

        // putting a named lambda provides a runnable (i.e. will accept no args)
        if ("put".equals( runnableKey )) {
            if ( params.length != 2
                    || ! ( params[ 0 ] instanceof String )
                    || ! ( params[ 1 ] instanceof LambdaExpression ) )
            {
                return null;
            }
            putAsRunnable( context, params, root );
            context.setPropertyResolved( base, methodName );
            return base;
        }

        if ( !root.containsKey( runnableKey ) )
        {
            return null;
        }

        Object runnable = root.get( runnableKey );
        if ( runnable instanceof LambdaExpression )
        {
            Object result = ( ( LambdaExpression ) runnable ).invoke( context, params );
            context.setPropertyResolved( base, methodName );
            return result;
        }
        else if ( runnable instanceof Runnable )
        {
            ( ( Runnable ) runnable ).run();
            context.setPropertyResolved( base, methodName );
            return null;
        }
        return null;
    }


    private void putAsRunnable( ELContext localContext, Object[] params, Map< String, Object > base )
    {
        String key = ( String ) params[ 0 ];
        LambdaExpression action = ( LambdaExpression ) params[ 1 ];
        Runnable runnableAction = () -> action.invoke( localContext );
        base.put( key, runnableAction );
    }
}
