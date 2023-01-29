package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.MapELResolver;

import java.util.Map;
import java.util.Stack;

public class ThreadLocalStackELResolver extends MapELResolver
{
    private final ThreadLocal< Stack<Map<String,Object>> > scopeStack;

    private final ELResolver delegate = new MapELResolver();

    public ThreadLocalStackELResolver(ThreadLocal< Stack<Map<String,Object>> > scopeStack)
    {
        if (scopeStack == null) {
            throw new IllegalArgumentException("ThreadLocalStack must not be null");
        }
        this.scopeStack = scopeStack;
    }

    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        if ( base == null )
        {
            base = scopeStack.get().peek();
        }
        if (context == null) {
            throw new NullPointerException();
        }
        if (base instanceof Map && ( ( Map<?, ?> ) base ).containsKey( property )) {
            context.setPropertyResolved(base, property);
            Map<?, ?> map = (Map<?, ?>) base;
            return map.get(property);
        }

        return null;
    }
}
