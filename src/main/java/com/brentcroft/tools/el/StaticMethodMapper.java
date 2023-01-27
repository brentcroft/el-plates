package com.brentcroft.tools.el;

import java.lang.reflect.Method;

public interface StaticMethodMapper
{
    void mapFunction( String prefixedName, Method staticMethod );
}
