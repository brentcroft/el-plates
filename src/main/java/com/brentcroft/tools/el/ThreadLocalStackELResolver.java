package com.brentcroft.tools.el;

import com.brentcroft.tools.jstl.MapBindings;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.MapELResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class ThreadLocalStackELResolver extends MapELResolver
{
    private final ThreadLocal< Stack<Map<String,Object>> > scopeStack;
    private Evaluator evaluator;
    private TextExpander expander;

    public ThreadLocalStackELResolver(TextExpander expander, Evaluator evaluator, ThreadLocal< Stack<Map<String,Object>> > scopeStack)
    {
        if ( expander == null )
        {
            throw new IllegalArgumentException("Expander must not be null");
        }
        if ( evaluator == null )
        {
            throw new IllegalArgumentException("Evaluator must not be null");
        }
        if (scopeStack == null) {
            throw new IllegalArgumentException("ThreadLocalStack must not be null");
        }
        this.expander = expander;
        this.evaluator = evaluator;
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

    public Object invoke(ELContext context, Object base, Object methodName, Class<?>[] paramTypes, Object[] params) {
        if (base == null || methodName == null) {
            return null;
        }
        if (!(base instanceof Map)) {
            return null;
        }

        String stepsKey = format("$$%s", methodName);

        if (!( ( Map<?, ?> ) base ).containsKey( stepsKey ) ) {
            return null;
        }

        if (params != null && params.length > 0  && !(params[0] instanceof Map)) {
            throw new IllegalArgumentException("Steps call must have one argument that is a map, or no argument at all");
        }

        String steps = format("%s",( ( Map<?, ?> ) base ).get( stepsKey ) );

        Map<String, Object> root = (Map<String,Object>)base;
        Map<String, Object> scope =
                params != null && params.length > 0
                ? (Map<String,Object>)params[0]
                : new HashMap<>();

        Map< String, Object > container = newContainer(root);
        scopeStack.get().push(scope);
        try
        {
            Object[] lastResult = {null};
            stepsStream(steps)
                    .map( step -> expander.expandText( step, container ) )
                    .forEachOrdered( step -> lastResult[0] = evaluator.eval( steps, container ) );
            Object ret = lastResult[0];
            context.setPropertyResolved( base, methodName );
            return ret;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ReturnException) {
                return ((ReturnException)e.getCause()).get();
            } else if (e.getCause() != null && e.getClass().isAssignableFrom( e.getCause().getClass() )) {
                throw (RuntimeException)e.getCause();
            }
            throw e;
        } finally {
            scopeStack.get().pop();
        }
    }

    public Map< String, Object > newContainer(Map<String, Object> root)
    {
        MapBindings bindings = new MapBindings(root);
        bindings.put( "$local", scopeStack.get().peek() );
        bindings.put( "$this", root );
        return bindings;
    }

    static Stream<String> stepsStream( String value) {
        String uncommented = Stream
                .of(value.split( "\\s*[\\n\\r]+\\s*" ))
                .filter( v -> !v.isEmpty() && !v.startsWith( "#" ) )
                .map( String::trim )
                .collect( Collectors.joining(" "));
        return Stream
                .of(uncommented.split( "\\s*[;]+\\s*" ));
    }
}
