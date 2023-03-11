package com.brentcroft.tools.el.resolver;

import com.brentcroft.tools.jstl.MapBindings;
import jakarta.el.ELContext;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Stack;

import static java.lang.String.format;

@AllArgsConstructor
public class ThreadLocalRootResolver extends BaseELResolver
{
    private final ThreadLocal< Stack< MapBindings > > scopeStack;
    private final static String localProperty = "$local";

    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        context.setPropertyResolved( false );

        if ( property == null )
        {
            return null;
        }
        else if ( localProperty.equals( property ) )
        {
            // indicates a scope for assignment
            return findNextScopeStack( context, base );
        }
        else if ( base != null )
        {
            return null;
        }
        else
        {
            return findPropertyInStackScope( context, base, property.toString() );
        }
    }

    private Object findNextScopeStack( ELContext context, Object base )
    {
        final Stack< MapBindings > stack = scopeStack.get();
        for ( int i = stack.size() - 1; i >= 0; i-- )
        {
            final Map< String, Object > scope = stack.get( i );
            if ( scope.containsKey( localProperty ) )
            {
                context.setPropertyResolved( base, localProperty );
                return scopeStack.get().peek();
            }
        }
        throw new IllegalArgumentException( format( "No scope contains key:  %s", localProperty ) );
    }

    private Object findPropertyInStackScope( ELContext context, Object base, String property )
    {
        final Stack< MapBindings > stack = scopeStack.get();
        for ( int i = stack.size() - 1; i >= 0; i-- )
        {
            final Map< String, Object > scope = stack.get( i );
            if ( scope.containsKey( property ) )
            {
                context.setPropertyResolved( base, property );
                return scope.get( property );
            }
        }
        return null;
    }
}
