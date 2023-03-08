package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;

@AllArgsConstructor
public class ConditionalMethodsELResolver extends BaseELResolver
{
    private final ThreadLocal< Stack< Map< String, Object > > > scopeStack;

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

    @SuppressWarnings( "unchecked" )
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

        Map< String, Object > baseMap = ( Map< String, Object > ) base;

        switch ( methodName.toString() )
        {
            case "ifThen":
                if ( params.length < 2
                        || ! ( params[ 0 ] instanceof LambdaExpression )
                        || ! ( params[ 1 ] instanceof LambdaExpression ) )
                {
                    return null;
                }
                scopeStack.get().push( newContainer( baseMap ) );
                try
                {
                    ifThen( context, params );
                    context.setPropertyResolved( base, methodName );
                    return base;
                }
                finally
                {
                    scopeStack.get().pop();
                }

            case "ifThenElse":
                if ( params.length < 3
                        || ! ( params[ 0 ] instanceof LambdaExpression )
                        || ! ( params[ 1 ] instanceof LambdaExpression )
                        || ! ( params[ 2 ] instanceof LambdaExpression ) )
                {
                    return null;
                }
                scopeStack.get().push( newContainer( baseMap ) );
                try
                {
                    ifThenElse( context, params );
                    context.setPropertyResolved( base, methodName );
                    return base;
                }
                finally
                {
                    scopeStack.get().pop();
                }

            case "whileDo":
                if ( params.length < 3
                        || ! ( params[ 0 ] instanceof LambdaExpression )
                        || ! ( params[ 1 ] instanceof LambdaExpression )
                        || ! ( params[ 2 ] instanceof Number ) )
                {
                    return null;
                }
                scopeStack.get().push( newContainer( baseMap ) );
                try
                {
                    whileDo( context, params );
                    context.setPropertyResolved( base, methodName );
                    return base;
                }
                finally
                {
                    scopeStack.get().pop();
                }

            case "tryExcept":
                if ( params.length < 2
                        || ! ( params[ 0 ] instanceof LambdaExpression )
                        || ! ( params[ 1 ] instanceof LambdaExpression ) )
                {
                    return null;
                }
                scopeStack.get().push( newContainer( baseMap ) );
                try
                {
                    tryExcept( context, params );
                    context.setPropertyResolved( base, methodName );
                    return base;
                }
                finally
                {
                    scopeStack.get().pop();
                }

            case "put":
                if ( params.length != 2
                        || ! ( params[ 0 ] instanceof String )
                        || ! ( params[ 1 ] instanceof LambdaExpression ) )
                {
                    return null;
                }

                putRunnable( context, params, baseMap );
                context.setPropertyResolved( base, methodName );
                return base;
        }


        return null;
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
        long started = System.currentTimeMillis();
        try
        {
            LambdaExpression ops = ( LambdaExpression ) params[ 0 ];
            ops.invoke( context );
        }
        catch ( Exception handled )
        {
            LambdaExpression onEx = ( LambdaExpression ) params[ 1 ];
            onEx.invoke( context, skipOrRaise( handled ) );
        }
        finally
        {
            double durationSeconds = Long
                    .valueOf( System.currentTimeMillis() - started ).doubleValue() / 1000;
            Optional
                    .ofNullable(
                            ( params.length > 2 && params[ 2 ] instanceof LambdaExpression )
                            ? ( LambdaExpression ) params[ 2 ]
                            : null
                    )
                    .ifPresent( le -> le.invoke( context, durationSeconds ) );
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
        while ( returnHandlingTest.apply( context, test ) )
        {
            currentLoop++;
            if ( currentLoop > maxLoops )
            {
                double durationSeconds = Long
                        .valueOf( System.currentTimeMillis() - started ).doubleValue() / 1000;
                onTimeout
                        .orElseThrow( () -> new RetriesException( maxLoops, test.toString() ) )
                        .invoke( context, durationSeconds );
                return;
            }
            ops.invoke( context, currentLoop );
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
        if ( returnHandlingTest.apply( context, test ) )
        {
            final LambdaExpression thenOps = ( LambdaExpression ) params[ 1 ];
            thenOps.invoke( context );
        }
        else
        {
            final LambdaExpression elseOps = ( LambdaExpression ) params[ 2 ];
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
        if ( returnHandlingTest.apply( context, test ) )
        {
            final LambdaExpression thenOps = ( LambdaExpression ) params[ 1 ];
            thenOps.invoke( context );
        }
    }
}
