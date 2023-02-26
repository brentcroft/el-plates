package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.LambdaExpression;
import jakarta.el.MapELResolver;
import lombok.AllArgsConstructor;

import java.util.Map;

import static java.lang.String.format;

@AllArgsConstructor
public class MapMethodELResolver extends MapELResolver
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
}
