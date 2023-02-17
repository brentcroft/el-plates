package com.brentcroft.tools.el;

import com.brentcroft.tools.jstl.MapBindings;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import jakarta.el.MapELResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static java.lang.String.format;

public class ThreadLocalStackELResolver extends MapELResolver
{
    private final ThreadLocal< Stack< Map< String, Object > > > scopeStack;
    private final Evaluator evaluator;
    private final TextExpander expander;
    private final Map< String, Object > staticMap;

    public ThreadLocalStackELResolver( TextExpander expander, Evaluator evaluator, ThreadLocal< Stack< Map< String, Object > > > scopeStack, Map< String, Object > staticMap )
    {
        if ( expander == null )
        {
            throw new IllegalArgumentException( "Expander must not be null" );
        }
        if ( evaluator == null )
        {
            throw new IllegalArgumentException( "Evaluator must not be null" );
        }
        if ( scopeStack == null )
        {
            throw new IllegalArgumentException( "ThreadLocalStack must not be null" );
        }
        this.expander = expander;
        this.evaluator = evaluator;
        this.scopeStack = scopeStack;
        this.staticMap = staticMap;
    }

    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        if ( base == null )
        {
            base = scopeStack.get().peek();
        }
        if ( context == null )
        {
            throw new NullPointerException();
        }
        if ( base instanceof Map && ( ( Map< ?, ? > ) base ).containsKey( property ) )
        {
            context.setPropertyResolved( base, property );
            Map< ?, ? > map = ( Map< ?, ? > ) base;
            return map.get( property );
        }

        return null;
    }

    public Object invoke( ELContext context, Object base, Object methodName, Class< ? >[] paramTypes, Object[] params )
    {
        if ( base == null || methodName == null )
        {
            return null;
        }
        if ( ! ( base instanceof Map ) )
        {
            return null;
        }

        @SuppressWarnings( "unchecked" )
        Map< String, Object > root = ( Map< String, Object > ) base;

        String runnableKey = format( "%s", methodName );
        if ( root.containsKey( runnableKey ) )
        {
            Object runnable = root.get( runnableKey );
            if ( runnable instanceof LambdaExpression )
            {
                Object result = ( ( LambdaExpression ) runnable ).invoke( context, params );
                context.setPropertyResolved( base, methodName );
                return result;
            }
            else if ( runnable instanceof Runnable )
            {
                ( ( Runnable ) runnable ).run();
                context.setPropertyResolved( base, methodName );
                return null;
            }
        }

        String stepsKey = format( "$$%s", methodName );

        if ( ! root.containsKey( stepsKey ) )
        {
            return null;
        }

        if ( params != null && params.length > 0 && ! ( params[ 0 ] instanceof Map ) )
        {
            throw new IllegalArgumentException( "Steps call must have one argument that is a map, or no argument at all" );
        }

        String steps = format( "%s", root.get( stepsKey ) );

        @SuppressWarnings( "unchecked" )
        Map< String, Object > scope =
                params != null && params.length > 0
                ? ( Map< String, Object > ) params[ 0 ]
                : new HashMap<>();

        scope.put( "$functionName", stepsKey );

        scopeStack.get().push( scope );
        try
        {
            Map< String, Object > container = newContainer( root );
            Object[] lastResult = { null };
            Evaluator
                    .stepsStream( steps )
                    .map( step -> expander.expandText( step, container ) )
                    .forEachOrdered( step -> lastResult[ 0 ] = evaluator.eval( step, container ) );
            Object ret = lastResult[ 0 ];
            context.setPropertyResolved( base, methodName );
            return ret;

        }
        catch ( ReturnException e )
        {
            context.setPropertyResolved( base, methodName );
            return e.get();

        }
        catch ( RuntimeException e )
        {
            RuntimeException cause = e;
            while ( cause.getCause() != null && cause.getCause() instanceof ELException )
            {
                cause = ( ELException ) cause.getCause();
            }
            if ( cause instanceof ReturnException )
            {
                context.setPropertyResolved( base, methodName );
                return ( ( ReturnException ) cause ).get();
            }

            throw e;
        }
        finally
        {
            scopeStack.get().pop();
        }
    }

    public Map< String, Object > newContainer( Map< String, Object > root )
    {
        MapBindings bindings = new MapBindings( root );
        bindings.put( "$local", scopeStack.get().peek() );
        bindings.put( "$self", root );
        if ( root instanceof Parented )
        {
            bindings.put( "$parent", ( ( Parented ) root ).getParent() );
        }
        if ( staticMap != null )
        {
            bindings.put( "$static", staticMap );
        }
        return bindings;
    }
}
