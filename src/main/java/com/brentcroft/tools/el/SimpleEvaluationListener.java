package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.EvaluationListener;

public class SimpleEvaluationListener extends EvaluationListener
{
    @Override
    public void beforeEvaluation( ELContext context, String expression )
    {
        System.out.printf( "before: %s%n", expression );
    }

    @Override
    public void afterEvaluation( ELContext context, String expression )
    {
        System.out.printf( "after: %s%n", expression );
    }

    public void propertyResolved( ELContext context, Object base, Object property )
    {
        System.out.printf( "property resolved: %s%n", property );
    }
}
