package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.MapELResolver;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class SimpleMapELResolver extends BaseELResolver
{
    private final Map< ?, ? > rootObjects;

    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        if ( base == null )
        {
            base = rootObjects;
        }
        if ( context == null )
        {
            throw new NullPointerException();
        }
        if ( base instanceof Map && ( ( Map< ?, ? > ) base ).containsKey( property ) )
        {
            context.setPropertyResolved( base, property );
            Map< ?, ? > map = ( Map< ?, ? > ) base;
            return map.get( property );
        }

        return null;
    }
}
