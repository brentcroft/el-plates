package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.VariableMapper;
import lombok.Getter;

import java.util.Map;

class SimpleELContext extends ELContext
{
    @Getter
    protected final FunctionMapper functionMapper;

    @Getter
    protected final VariableMapper variableMapper;

    private final Map< ?, ? > rootObjects;

    protected ELResolver resolver;

    public SimpleELContext( SimpleELContextFactory simpleELContextFactory, Map< ?, ? > rootObjects )
    {
        this.functionMapper = simpleELContextFactory.newFunctionMapper();
        this.variableMapper = SimpleELContextFactory.newVariableMapper();
        this.rootObjects = rootObjects;
        this.resolver = simpleELContextFactory.newResolver( rootObjects );
    }

    @Override
    public ELResolver getELResolver()
    {
        return resolver;
    }
}
