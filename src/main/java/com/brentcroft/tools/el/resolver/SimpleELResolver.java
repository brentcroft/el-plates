package com.brentcroft.tools.el.resolver;

import com.brentcroft.tools.el.ELTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;
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
        add( new FixedMapELResolver( rootObjects ) );
        add( new ThreadLocalRootResolver( scopeStack ) );

        add( new SimpleMapELResolver() );
        add( new MapMethodELResolver() );

        add( new StreamELResolver() );
        add( new StaticFieldELResolver() );
        add( new ArrayELResolver() );
        add( new ListELResolver() );

        add( new MapStepsELResolver( scopeStack, em, em ) );
        add( new ConditionalMethodsELResolver( scopeStack, em.getELContextFactory() ) );
        add( new StaticMapELResolver( staticModel ) );

        add( new BeanELResolver() );
        add( new ResourceBundleELResolver() );
    }
}
