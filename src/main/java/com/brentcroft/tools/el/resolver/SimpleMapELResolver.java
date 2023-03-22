package com.brentcroft.tools.el.resolver;

import jakarta.el.ELContext;
import jakarta.el.MapELResolver;

import java.util.Map;

public class SimpleMapELResolver extends MapELResolver
{
    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        if ( base instanceof Map && ( ( Map< ?, ? > ) base ).containsKey( property ) )
        {
            context.setPropertyResolved( base, property );
            Map< ?, ? > map = ( Map< ?, ? > ) base;
            return map.get( property );
        }

        return null;
    }
}
