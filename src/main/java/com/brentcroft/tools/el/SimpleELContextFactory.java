package com.brentcroft.tools.el;

import com.sun.el.stream.StreamELResolver;
import jakarta.el.*;
import lombok.extern.java.Log;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static com.brentcroft.tools.jstl.JstlNamespace.prefix;
import static java.lang.String.format;


/**
 * Factory for ELContext objects.
 *
 * @author ADobson
 */
@Log
public class SimpleELContextFactory implements ELContextFactory
{
    private final Map< String, Method > mappedFunctions = new HashMap<>();

    private CompositeELResolver customPrimaryResolvers;
    private CompositeELResolver customSecondaryResolvers;

    public void mapFunction( String unprefixedName, Method staticMethod )
    {
        mappedFunctions.put( prefix( unprefixedName ), staticMethod );
    }

    public void mapFunction( String prefix, String unprefixedName, Method staticMethod )
    {
        mappedFunctions.put( prefix + ":" + unprefixedName, staticMethod );
    }

    public void mapFunctions( Map< String, Method > functions )
    {
        mappedFunctions.putAll( functions );
    }

    {
        ELFunctions.install( this::mapFunction );
        log.fine( this::listMappedFunctions );
    }

    public String listMappedFunctions()
    {
        return mappedFunctions
                .entrySet()
                .stream()
                .map( entry -> format( "\n  %1$-30s = %2$s", entry.getKey(), entry.getValue() ) )
                .collect( Collectors.joining() );
    }

    public ELContext getELContext( Map< ?, ? > rootObjects )
    {
        return new SimpleELContext( this, rootObjects );
    }

    public ELContext getELConfigContext()
    {
        return new RootELContext( null );
    }

    FunctionMapper newFunctionMapper()
    {
        return new FunctionMapper()
        {
            @Override
            public Method resolveFunction( String prefix, String localName )
            {
                return mappedFunctions.get( ( prefix == null ? "" : prefix + ":" ) + localName );
            }
        };
    }

    public void addPrimaryELResolver(ELResolver cELResolver) {
        if ( customPrimaryResolvers == null) {
            customPrimaryResolvers = new CompositeELResolver();
        }
        customPrimaryResolvers.add(cELResolver);
    }

    public void addSecondaryELResolver(ELResolver cELResolver) {
        if ( customSecondaryResolvers == null)
        {
            customSecondaryResolvers = new CompositeELResolver();
        }
        customSecondaryResolvers.add(cELResolver);
    }

    ELResolver newResolver(Map< ?, ? > rootObjects) {
        CompositeELResolver resolver = new CompositeELResolver();
        // eg: thread local stack
        if ( customPrimaryResolvers != null)
        {
            resolver.add( customPrimaryResolvers );
        }

        resolver.add( new SimpleELResolver( rootObjects ) );

        // eg: static maps
        if ( customSecondaryResolvers != null )
        {
            resolver.add( customSecondaryResolvers );
        }
        resolver.add( new StreamELResolver() );
        resolver.add( new StaticFieldELResolver() );
        resolver.add( new ArrayELResolver() );
        resolver.add( new ListELResolver() );
        resolver.add( new BeanELResolver() );
        resolver.add( new MapELResolver() );
        resolver.add( new ResourceBundleELResolver() );

        return resolver;
    }

    class RootELContext extends SimpleELContext
    {
        public RootELContext( Map< ?, ? > rootObjects )
        {
            super( SimpleELContextFactory.this, rootObjects );
        }
    }

    static VariableMapper newVariableMapper()
    {
        return new VariableMapper()
        {
            private Map< String, ValueExpression > variableMap = Collections.emptyMap();

            @Override
            public ValueExpression resolveVariable( String name )
            {
                return variableMap.get( name );
            }

            @Override
            public ValueExpression setVariable( String name, ValueExpression variable )
            {
                if ( variableMap.isEmpty() )
                {
                    variableMap = new HashMap<>();
                }
                return variableMap.put( name, variable );
            }
        };
    }
}
