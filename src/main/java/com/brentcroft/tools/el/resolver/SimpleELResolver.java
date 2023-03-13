package com.brentcroft.tools.el.resolver;

import com.brentcroft.tools.el.ELTemplateManager;
import com.brentcroft.tools.el.MapBindings;
import com.sun.el.stream.StreamELResolver;
import jakarta.el.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Stack;

@Getter
@Setter
public class SimpleELResolver extends CompositeELResolver
{
    public SimpleELResolver( Map< ?, ? > rootObjects, ThreadLocal< Stack< MapBindings > > scopeStack, Map< String, Object > staticModel, ELTemplateManager em )
    {
        // scopes
        add( new FixedMapELResolver( rootObjects ) );
        add( new ThreadLocalRootResolver( scopeStack ) );
        add( new StaticMapELResolver( staticModel ) );

        //
        add( new StreamELResolver() );
        add( new StaticFieldELResolver() );
        add( new ArrayELResolver() );
        add( new ListELResolver() );

        // only applies to base maps that have the required property
        add( new SimpleMapELResolver() );

        // synthetic methods
        add( new MapMethodELResolver() );
        add( new MapStepsELResolver( scopeStack, em, em ) );
        add( new ConditionalMethodsELResolver( scopeStack ) );

        //
        add( new BeanELResolver() );
        add( new ResourceBundleELResolver() );
    }
}
