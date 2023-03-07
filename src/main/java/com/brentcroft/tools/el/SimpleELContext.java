package com.brentcroft.tools.el;

import jakarta.el.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

class SimpleELContext extends ELContext
{
    @Getter
    protected final FunctionMapper functionMapper;

    @Getter
    protected final VariableMapper variableMapper;

    @Getter
    @Setter
    protected Map< ?, ? > rootObjects;

    protected SimpleELContextFactory simpleELContextFactory;
    protected ELResolver resolver;
    private final ImportHandler importHandler;

    public SimpleELContext( SimpleELContextFactory simpleELContextFactory, Map< ?, ? > rootObjects, EvaluationListener... listeners )
    {
        this.rootObjects = rootObjects;
        this.simpleELContextFactory = simpleELContextFactory;
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
