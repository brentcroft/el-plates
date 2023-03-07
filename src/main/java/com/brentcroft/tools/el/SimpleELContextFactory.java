package com.brentcroft.tools.el;

import jakarta.el.*;
import lombok.Getter;
import lombok.extern.java.Log;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    @Getter
    private final ImportHandler importHandler = new ImportHandler();

    private final Map< String, Method > mappedFunctions = new HashMap<>();

    private EvaluationListener[] listeners;

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

    public void setListeners( EvaluationListener... listeners )
    {
        this.listeners = listeners;
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
        return new SimpleELContext( this, rootObjects, listeners );
    }

    public ELContext getELContext( Map< ?, ? > rootObjects, SimpleELContext parent )
    {
        return new SimpleELContext( this, rootObjects, listeners );
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

    public void addPrimaryELResolver( ELResolver cELResolver )
    {
        if ( customPrimaryResolvers == null )
        {
            customPrimaryResolvers = new CompositeELResolver();
        }
        customPrimaryResolvers.add( cELResolver );
    }

    public void addSecondaryELResolver( ELResolver cELResolver )
    {
        if ( customSecondaryResolvers == null )
        {
            customSecondaryResolvers = new CompositeELResolver();
        }
        customSecondaryResolvers.add( cELResolver );
    }

    ELResolver newResolver( Map< ?, ? > rootObjects )
    {
        return new SimpleELResolver( rootObjects, customPrimaryResolvers, customSecondaryResolvers );
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
