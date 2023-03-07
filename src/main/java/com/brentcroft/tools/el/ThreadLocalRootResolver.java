package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Stack;

@AllArgsConstructor
public class ThreadLocalRootResolver extends BaseELResolver
{
    private final ThreadLocal< Stack< Map< String, Object > > > scopeStack;

    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        if (base != null || property == null || scopeStack.get().isEmpty()) {
            return null;
        }

        if ("$local".equals( property ) && !scopeStack.get().isEmpty()) {
            context.setPropertyResolved( null, property );
            return scopeStack.get().peek();
        }

        final Stack<Map< String, Object >> stack = scopeStack.get();
        final String key = property.toString();
        for (int i = stack.size() - 1; i >= 0; i--) {
            final Map< String, Object > scope = stack.get( i );
            if (scope.containsKey( key )) {
                context.setPropertyResolved( base, property );
                return scope.get( key );
            }
        }
        return null;
    }
}
