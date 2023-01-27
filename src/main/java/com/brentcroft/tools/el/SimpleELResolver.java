package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.MapELResolver;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;

public class SimpleELResolver extends ELResolver
{
    private final ELResolver delegate = new MapELResolver();

    private final Map< ?, ? > userMap;

    public SimpleELResolver( Map< ?, ? > rootObjects )
    {
        this.userMap = rootObjects;
    }

    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        if ( base == null )
        {
            base = userMap;
        }
        return delegate.getValue( context, base, property );
    }


    @Override
    public Class< ? > getCommonPropertyType( ELContext context, Object arg1 )
    {
        return delegate.getCommonPropertyType( context, arg1 );
    }

    @Override
    public Iterator< FeatureDescriptor > getFeatureDescriptors( ELContext context, Object arg1 )
    {
        return delegate.getFeatureDescriptors( context, arg1 );
    }

    @Override
    public Class< ? > getType( ELContext context, Object arg1, Object arg2 )
    {
        return delegate.getType( context, arg1, arg2 );
    }

    @Override
    public boolean isReadOnly( ELContext context, Object arg1, Object arg2 )
    {
        return delegate.isReadOnly( context, arg1, arg2 );
    }

    @Override
    public void setValue( ELContext context, Object arg1, Object arg2, Object arg3 )
    {
        delegate.setValue( context, arg1, arg2, arg3 );
    }
}
