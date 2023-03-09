package com.brentcroft.tools.el.resolver;

import jakarta.el.ELContext;
import jakarta.el.MapELResolver;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class StaticMapELResolver extends MapELResolver
{
    private Map< ?, ? > staticMap;


    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        if ( property == null )
        {
            return null;
        }

        if ( "$static".equals( property ) )
        {
            context.setPropertyResolved( base, property );
            return staticMap;
        }

        if ( base != null )
        {
            return null;
        }

        final String key = property.toString();
        if ( staticMap.containsKey( key ) )
        {
            context.setPropertyResolved( base, property );
            return staticMap.get( key );
        }
        return null;
    }
}
