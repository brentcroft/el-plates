package com.brentcroft.tools.el;

import jakarta.el.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

public class SimpleELContext extends ELContext
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

    public ELContext getChildContext( Map< String, Object> baseMap )
    {
        return new ELContext() {
            @Override
            public ELResolver getELResolver()
            {
                return simpleELContextFactory.newResolver( baseMap );
            }

            @Override
            public FunctionMapper getFunctionMapper()
            {
                return functionMapper;
            }

            @Override
            public VariableMapper getVariableMapper()
            {
                return variableMapper;
            }

            public ImportHandler getImportHandler() {
                return SimpleELContext.this.getImportHandler();
            }

            public List<EvaluationListener> getEvaluationListeners() {
                return SimpleELContext.this.getEvaluationListeners();
            }

            public boolean isLambdaArgument(String arg) {
                return SimpleELContext.this.isLambdaArgument(arg);
            }
            public Object getLambdaArgument(String arg)
            {
                return SimpleELContext.this.getLambdaArgument(arg);
            }

            public void enterLambdaScope(Map<String, Object> args)
            {
                SimpleELContext.this.enterLambdaScope( args );
            }
            public void exitLambdaScope() {
                SimpleELContext.this.exitLambdaScope();
            }
//
//            public Object getContext(Class<?> key) {
//                return SimpleELContext.this.getContext( key );
//            }
        };
    }
}
