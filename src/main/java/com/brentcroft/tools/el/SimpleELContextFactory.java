package com.brentcroft.tools.el;

import com.brentcroft.tools.el.resolver.SimpleELResolver;
import jakarta.el.*;
import lombok.Getter;
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
    private final ELTemplateManager el;
    @Getter
    private final ImportHandler importHandler = new ImportHandler();
    private final Map< String, Method > mappedFunctions = new HashMap<>();
    private EvaluationListener[] listeners;

    public SimpleELContextFactory(ELTemplateManager el) {
        this.el = el;
    }

    private static final ThreadLocal< Stack< MapBindings > > scopeStack = ThreadLocal.withInitial( () -> {
        MapBindings staticRoot = new MapBindings()
                .withEntry( "$local", null )
                .withEntry( "$functionName", "main" );

        Stack< MapBindings > s = new Stack<>();
        s.push( new MapBindings(staticRoot));
        return s;
    } );

    private static final Map< String, Object > staticModel = new LinkedHashMap<>();

    public ThreadLocal< Stack< MapBindings > > getScopeStack() {
        return scopeStack;
    }

    public Map< String, Object > getStaticModel() {
        return staticModel;
    }


    /**
     * Clear down the scope stack to one empty map.
     * Clear the static model.
     */
    public static void clean() {

        while(scopeStack.get().size() > 1) {
            scopeStack.get().pop();
        }
        if (!scopeStack.get().isEmpty()) {
            scopeStack.get().peek().clear();
        }
        staticModel.clear();
    }

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

    ELResolver newResolver( Map< ?, ? > rootObjects )
    {
        return new SimpleELResolver( rootObjects, scopeStack, staticModel, el );
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
