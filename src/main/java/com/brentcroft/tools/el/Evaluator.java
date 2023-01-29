package com.brentcroft.tools.el;

import java.util.Map;

public interface Evaluator
{
    Object eval( String expression, Map<String, Object> scope );
}
