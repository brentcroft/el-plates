package com.brentcroft.tools.el.resolver;

import com.brentcroft.tools.jstl.MapBindings;
import jakarta.el.ELContext;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Stack;

@AllArgsConstructor
public class ThreadLocalRootResolver extends BaseELResolver
{
    private final ThreadLocal< Stack< MapBindings > > scopeStack;

    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        if (property == null) {
            return null;
        }

        if ("$local".equals( property )) {
            context.setPropertyResolved( base, property );
            return scopeStack.get().peek();
        }

        if (base != null) {
            return null;
        }

        final Stack<MapBindings> stack = scopeStack.get();
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
