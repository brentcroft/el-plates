package com.brentcroft.tools.el;

import com.brentcroft.tools.jstl.MapBindings;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;

public class BaseELResolver extends ELResolver
{
    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        return null;
    }

    @Override
    public Class< ? > getType( ELContext context, Object base, Object property )
    {
        return null;
    }

    @Override
    public void setValue( ELContext context, Object base, Object property, Object value )
    {

    }

    @Override
    public boolean isReadOnly( ELContext context, Object base, Object property )
    {
        return false;
    }

    @Override
    public Iterator< FeatureDescriptor > getFeatureDescriptors( ELContext context, Object base )
    {
        return null;
    }

    @Override
    public Class< ? > getCommonPropertyType( ELContext context, Object base )
    {
        return null;
    }


    public Map< String, Object > newContainer( Map< String, Object > root )
    {
        MapBindings bindings = new MapBindings( root );
        bindings.put( "$local", bindings );
        bindings.put( "$self", root );
        if ( root instanceof Parented )
        {
            bindings.put( "$parent", ( ( Parented ) root ).getParent() );
        }
        return bindings;
    }
}
