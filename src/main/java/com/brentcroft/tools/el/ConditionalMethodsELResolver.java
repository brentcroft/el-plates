package com.brentcroft.tools.el;

import com.brentcroft.tools.jstl.MapBindings;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import jakarta.el.MapELResolver;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;

@AllArgsConstructor
public class ConditionalMethodsELResolver extends MapELResolver
{
    private ELContextFactory contextFactory;
    private final ThreadLocal< Stack< Map< String, Object > > > scopeStack;
    private final Map< String, Object > staticMap;

    private static final BiFunction< ELContext, LambdaExpression, Boolean > returnHandlingTest = ( ELContext context, LambdaExpression test ) -> {
        try
        {
            return ( Boolean ) test.invoke( context );
        }
        catch ( ELException e )
        {
            ELException cause = e;
            while ( cause.getCause() != null && cause.getCause() instanceof ELException )
            {
                cause = ( ELException ) cause.getCause();
            }
            if ( cause instanceof ReturnException )
            {
                return Boolean.parseBoolean( ( ( ReturnException ) cause ).get().toString() );
            }
            throw cause;
        }
    };


    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        return null;
    }

    @SuppressWarnings( "unchecked" )
    public Object invoke( ELContext context, Object base, Object methodName, Class< ? >[] paramTypes, Object[] params )
    {
        if ( base == null || methodName == null )
        {
            return null;
        }
        if ( !(base instanceof Map) )
        {
            return null;
        }

        Map< String, Object > baseMap = ( Map< String, Object > ) base;

        ELContext localContext = contextFactory.getELContext( newContainer( baseMap ) );

        switch ( methodName.toString() )
        {
            case "ifThen":
                if ( params.length < 2
                        || ! ( params[ 0 ] instanceof LambdaExpression )
                        || ! ( params[ 1 ] instanceof LambdaExpression ) )
                {
                    return null;
                }
                ifThen( localContext, params );
                context.setPropertyResolved( base, methodName );
                return base;

            case "ifThenElse":
                if ( params.length < 3
                        || ! ( params[ 0 ] instanceof LambdaExpression )
                        || ! ( params[ 1 ] instanceof LambdaExpression )
                        || ! ( params[ 2 ] instanceof LambdaExpression ) )
                {
                    return null;
                }
                ifThenElse( localContext, params );
                context.setPropertyResolved( base, methodName );
                return base;

            case "whileDo":
                if ( params.length < 3
                        || ! ( params[ 0 ] instanceof LambdaExpression )
                        || ! ( params[ 1 ] instanceof LambdaExpression )
                        || ! ( params[ 2 ] instanceof Number ) )
                {
                    return null;
                }
                whileDo( localContext, params );
                context.setPropertyResolved( base, methodName );
                return base;

            case "tryExcept":
                if ( params.length < 2
                        || ! ( params[ 0 ] instanceof LambdaExpression )
                        || ! ( params[ 1 ] instanceof LambdaExpression ) )
                {
                    return null;
                }
                tryExcept( localContext, params );
                context.setPropertyResolved( base, methodName );
                return base;


            case "tryExceptFinally":
                if ( params.length < 3
                        || ! ( params[ 0 ] instanceof LambdaExpression )
                        || ! ( params[ 1 ] instanceof LambdaExpression )
                        || ! ( params[ 2 ] instanceof LambdaExpression )
                )
                {
                    return null;
                }
                tryExceptFinally( localContext, params );
                context.setPropertyResolved( base, methodName );
                return base;

            case "put":
                if ( params.length != 2
                        || ! ( params[ 0 ] instanceof String )
                        || ! ( params[ 1 ] instanceof LambdaExpression ) )
                {
                    return null;
                }

                putRunnable( localContext, params, baseMap );
                context.setPropertyResolved( base, methodName );
                return base;
        }


        return null;
    }

    public Map< String, Object > newContainer( Map< String, Object > owner )
    {
        MapBindings bindings = new MapBindings( owner );
        bindings.put( "$local", scopeStack.get().peek() );
        bindings.put( "$self", owner );
        if ( staticMap != null )
        {
            bindings.put( "$static", staticMap );
        }
        if ( owner instanceof Parented )
        {
            bindings.put( "$parent", ( ( Parented ) owner ).getParent() );
        }
        return bindings;
    }

    private void putRunnable( ELContext localContext, Object[] params, Map< String, Object > base )
    {
        String key = ( String ) params[ 0 ];
        LambdaExpression action = ( LambdaExpression ) params[ 1 ];
        Runnable runnableAction = () -> action.invoke( localContext );
        base.put( key, runnableAction );
    }

    private void tryExcept( ELContext context, Object[] params )
    {
        if ( params.length < 2 || ! ( params[ 0 ] instanceof LambdaExpression ) || ! ( params[ 1 ] instanceof LambdaExpression ) )
        {
            throw new ELException( "Must have arguments: tryExcept( LambdaExpression, LambdaExpression )" );
        }
        LambdaExpression ops = ( LambdaExpression ) params[ 0 ];
        LambdaExpression onEx = ( LambdaExpression ) params[ 1 ];
        try
        {
            ops.invoke( context );
        }
        catch ( Exception handled )
        {
            onEx.invoke( context, skipOrRaise( handled ) );
        }
    }

    private void tryExceptFinally( ELContext context, Object[] params )
    {
        if ( params.length < 3
                || ! ( params[ 0 ] instanceof LambdaExpression )
                || ! ( params[ 1 ] instanceof LambdaExpression )
                || ! ( params[ 2 ] instanceof LambdaExpression ) )
        {
            throw new ELException( "Must have arguments: tryExceptFinally( LambdaExpression, LambdaExpression, LambdaExpression )" );
        }
        LambdaExpression ops = ( LambdaExpression ) params[ 0 ];
        LambdaExpression onEx = ( LambdaExpression ) params[ 1 ];
        LambdaExpression finOps = ( LambdaExpression ) params[ 2 ];
        try
        {
            ops.invoke( context );
        }
        catch ( Exception handled )
        {
            onEx.invoke( context, skipOrRaise( handled ) );
        }
        finally
        {
            finOps.invoke( context );
        }
    }

    private Exception skipOrRaise( Exception cause )
    {
        while ( cause.getCause() != null && cause instanceof ELException )
        {
            cause = ( Exception ) cause.getCause();
        }
        if ( cause instanceof ReturnException )
        {
            throw ( ReturnException ) cause;
        }
        return cause;
    }

    private void whileDo( ELContext context, Object[] params )
    {
        if ( params.length < 3
                || ! ( params[ 0 ] instanceof LambdaExpression )
                || ! ( params[ 1 ] instanceof LambdaExpression )
                || ! ( params[ 2 ] instanceof Number ) )
        {
            throw new ELException( "Must have arguments: whileDo( LambdaExpression, LambdaExpression, Number[, LambdaExpression] )" );
        }
        final LambdaExpression test = ( LambdaExpression ) params[ 0 ];
        final LambdaExpression ops = ( LambdaExpression ) params[ 1 ];
        int maxLoops = ( ( Number ) params[ 2 ] ).intValue();
        int currentLoop = 0;

        Optional< LambdaExpression > onTimeout = Optional
                .ofNullable(
                        ( params.length > 3 && params[ 3 ] instanceof LambdaExpression )
                        ? ( LambdaExpression ) params[ 3 ]
                        : null
                );

        long started = System.currentTimeMillis();
        while ( returnHandlingTest.apply( context, test ) && maxLoops > currentLoop )
        {
            currentLoop++;
            ops.invoke( context );
        }
        if ( currentLoop >= maxLoops )
        {
            double durationSeconds = Long
                    .valueOf( System.currentTimeMillis() - started).doubleValue() / 1000;

            onTimeout
                    .orElseThrow( () -> new RetriesException( maxLoops, test.toString() ) )
                    .invoke( context, durationSeconds );
        }
    }

    private void ifThenElse( ELContext context, Object[] params )
    {
        if ( params.length < 3
                || ! ( params[ 0 ] instanceof LambdaExpression )
                || ! ( params[ 1 ] instanceof LambdaExpression )
                || ! ( params[ 2 ] instanceof LambdaExpression ) )
        {
            throw new ELException( "Must have arguments: ifThenElse( LambdaExpression, LambdaExpression, LambdaExpression )" );
        }
        final LambdaExpression test = ( LambdaExpression ) params[ 0 ];
        final LambdaExpression thenOps = ( LambdaExpression ) params[ 1 ];
        final LambdaExpression elseOps = ( LambdaExpression ) params[ 2 ];

        if ( returnHandlingTest.apply( context, test ) )
        {
            thenOps.invoke( context );
        }
        else
        {
            elseOps.invoke( context );
        }
    }

    private void ifThen( ELContext context, Object[] params )
    {
        if ( params.length < 2
                || ! ( params[ 0 ] instanceof LambdaExpression )
                || ! ( params[ 1 ] instanceof LambdaExpression ) )
        {
            throw new ELException( "Must have arguments: ifThen( LambdaExpression, LambdaExpression )" );
        }
        final LambdaExpression test = ( LambdaExpression ) params[ 0 ];
        final LambdaExpression thenOps = ( LambdaExpression ) params[ 1 ];

        if ( returnHandlingTest.apply( context, test ) )
        {
            thenOps.invoke( context );
        }
    }
}
