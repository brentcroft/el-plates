package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.MapELResolver;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class ParentedMapELResolver extends MapELResolver
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

        if ( base instanceof Parented && base != rootObjects )
        {
            return getValue( context, ( ( Parented ) base ).getParent(), property );
        }

        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException();
        }

        if ( base instanceof Map ) {
            context.setPropertyResolved(true);
            return Object.class;
        }

        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        if ( base instanceof Map ) {
            return Object.class;
        }

        return null;
    }
}
