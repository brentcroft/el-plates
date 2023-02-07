package com.brentcroft.tools.el;

import jakarta.el.*;
import lombok.Getter;

import java.util.Map;

class SimpleELContext extends ELContext
{
    @Getter
    protected final FunctionMapper functionMapper;

    @Getter
    protected final VariableMapper variableMapper;

    protected ELResolver resolver;

    private final ImportHandler importHandler;

    public SimpleELContext( SimpleELContextFactory simpleELContextFactory, Map< ?, ? > rootObjects, EvaluationListener... listeners )
    {
        this.functionMapper = simpleELContextFactory.newFunctionMapper();
        this.variableMapper = SimpleELContextFactory.newVariableMapper();
        this.resolver = simpleELContextFactory.newResolver( rootObjects );
        this.importHandler = simpleELContextFactory.getImportHandler();
        if (listeners != null) {
            for (EvaluationListener el : listeners) {
                addEvaluationListener( el );
            }
        }
    }

    @Override
    public ELResolver getELResolver()
    {
        return resolver;
    }

    public ImportHandler getImportHandler() {
        if (importHandler == null) {
            return super.getImportHandler();
        }
        return importHandler;
    }
}
