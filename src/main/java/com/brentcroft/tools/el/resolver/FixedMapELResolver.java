package com.brentcroft.tools.el.resolver;

import jakarta.el.ELContext;
import jakarta.el.MapELResolver;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class FixedMapELResolver extends MapELResolver
{
    private Map< ?, ? > rootObjects;

    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        if ( base == null && rootObjects.containsKey( property ) )
        {
            context.setPropertyResolved( null, property );
            return rootObjects.get( property );
        }

        return null;
    }
}
