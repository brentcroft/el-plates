package com.brentcroft.tools.el;

import com.sun.el.stream.StreamELResolver;
import jakarta.el.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SimpleELResolver extends CompositeELResolver
{
    private Map< ?, ? > rootObjects;

    public SimpleELResolver( Map<?,?> rootObjects, CompositeELResolver customPrimaryResolvers, CompositeELResolver customSecondaryResolvers )
    {
        this.rootObjects= rootObjects;

        // eg: thread local stack
        if ( customPrimaryResolvers != null )
        {
            add( customPrimaryResolvers );
        }

        add( new ParentedMapELResolver( rootObjects ) );

        // eg: static maps
        if ( customSecondaryResolvers != null )
        {
            add( customSecondaryResolvers );
        }

        add( new StreamELResolver() );
        add( new StaticFieldELResolver() );
        add( new ArrayELResolver() );
        add( new ListELResolver() );
        add( new BeanELResolver() );
        add( new MapELResolver() );
        add( new ResourceBundleELResolver() );
    }

    public void setValue( ELContext context, Object base, Object property, Object value )
    {
        super.setValue( context, base == null ? rootObjects : base, property, value );
    }
}
