package com.brentcroft.tools.el.resolver;

import com.brentcroft.tools.el.RetriesException;
import com.brentcroft.tools.el.ReturnException;
import com.brentcroft.tools.el.MapBindings;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.Stack;

@AllArgsConstructor
public class ConditionalMethodsELResolver extends BaseELResolver
{
    private final ThreadLocal< Stack< MapBindings > > scopeStack;

    @SuppressWarnings( "unchecked" )
    public Object invoke( ELContext context, Object base, Object methodName, Class< ? >[] paramTypes, Object[] params )
    {
        if ( methodName == null || ! ( base instanceof Map ) )
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
                ifThen( new ContextAndRoot(context, baseMap), params );
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
                ifThenElse( new ContextAndRoot(context, baseMap), params );
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
                whileDo( new ContextAndRoot(context, baseMap), params );
                context.setPropertyResolved( base, methodName );
                return base;

            case "tryExcept":
                if ( params.length < 2
                        || ! ( params[ 0 ] instanceof LambdaExpression )
                        || ! ( params[ 1 ] instanceof LambdaExpression ) )
                {
                    return null;
                }
                tryExcept( new ContextAndRoot(context, baseMap), params );
                context.setPropertyResolved( base, methodName );
                return base;
        }

        return null;
    }

    @Getter
    public class ContextAndRoot {
        private final ELContext localContext;
        private final MapBindings bindings;

        public ContextAndRoot(ELContext context, Map< String, Object > baseMap) {
            this.localContext = context;
            this.bindings = baseMap == null
                            ? null
                            : newContainer(  baseMap );
        }
    }

    private void maybePushRootMap( MapBindings scope )
    {
        if ( scope != null )
        {
            scopeStack.get().push( newContainer( scope ) );
        }
    }

    private void maybePopRootMap( Map< String, Object > rootMap )
    {
        if ( rootMap != null )
        {
            scopeStack.get().pop();
        }
    }

    private boolean returnHandlingTest( ELContext context, LambdaExpression test )  {
        try
        {
            return ( boolean ) test.invoke( context );
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

    private void tryExcept( ContextAndRoot cr, Object[] params )
    {
        if ( params.length < 2 || ! ( params[ 0 ] instanceof LambdaExpression ) || ! ( params[ 1 ] instanceof LambdaExpression ) )
        {
            throw new ELException( "Must have arguments: tryExcept( LambdaExpression, LambdaExpression )" );
        }
        maybePushRootMap( cr.getBindings() );
        try
        {
            long started = System.currentTimeMillis();
            try
            {
                LambdaExpression ops = ( LambdaExpression ) params[ 0 ];
                ops.invoke( cr.getLocalContext() );
            }
            catch ( Exception handled )
            {
                LambdaExpression onEx = ( LambdaExpression ) params[ 1 ];
                onEx.invoke( cr.getLocalContext(), skipOrRaise( handled ) );
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
                        .ifPresent( le -> le.invoke( cr.getLocalContext(), durationSeconds ) );
            }
        }
        finally
        {
            maybePopRootMap( cr.getBindings() );
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

    private void whileDo( ContextAndRoot cr, Object[] params )
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
        maybePushRootMap( cr.getBindings() );
        try
        {
            long started = System.currentTimeMillis();

            if ( currentLoop >= maxLoops )
            {
                double durationSeconds = Long
                        .valueOf( System.currentTimeMillis() - started ).doubleValue() / 1000;
                onTimeout
                        .orElseThrow( () -> new RetriesException( maxLoops, durationSeconds, test.toString() ) )
                        .invoke( cr.getLocalContext(), durationSeconds );
                return;
            }

            while ( returnHandlingTest( cr.getLocalContext(), test ) )
            {
                currentLoop++;
                if ( currentLoop > maxLoops )
                {
                    double durationSeconds = Long
                            .valueOf( System.currentTimeMillis() - started ).doubleValue() / 1000;
                    onTimeout
                            .orElseThrow( () -> new RetriesException( maxLoops, durationSeconds, test.toString() ) )
                            .invoke( cr.getLocalContext(), durationSeconds );
                    return;
                }
                ops.invoke( cr.getLocalContext(), currentLoop );
            }
        }
        finally
        {
            maybePopRootMap( cr.getBindings() );
        }
    }

    private void ifThenElse( ContextAndRoot cr, Object[] params )
    {
        if ( params.length < 3
                || ! ( params[ 0 ] instanceof LambdaExpression )
                || ! ( params[ 1 ] instanceof LambdaExpression )
                || ! ( params[ 2 ] instanceof LambdaExpression ) )
        {
            throw new ELException( "Must have arguments: ifThenElse( LambdaExpression, LambdaExpression, LambdaExpression )" );
        }
        maybePushRootMap( cr.getBindings() );
        try
        {
            final LambdaExpression test = ( LambdaExpression ) params[ 0 ];
            if ( returnHandlingTest( cr.getLocalContext(), test ) )
            {
                final LambdaExpression thenOps = ( LambdaExpression ) params[ 1 ];
                thenOps.invoke( cr.getLocalContext() );
            }
            else
            {
                final LambdaExpression elseOps = ( LambdaExpression ) params[ 2 ];
                elseOps.invoke( cr.getLocalContext() );
            }
        }
        finally
        {
            maybePopRootMap( cr.getBindings() );
        }
    }

    private void ifThen( ContextAndRoot cr, Object[] params )
    {
        if ( params.length < 2
                || ! ( params[ 0 ] instanceof LambdaExpression )
                || ! ( params[ 1 ] instanceof LambdaExpression ) )
        {
            throw new ELException( "Must have arguments: ifThen( LambdaExpression, LambdaExpression )" );
        }
        maybePushRootMap( cr.getBindings() );
        try
        {
            final LambdaExpression test = ( LambdaExpression ) params[ 0 ];
            if ( returnHandlingTest( cr.getLocalContext(), test ) )
            {
                final LambdaExpression thenOps = ( LambdaExpression ) params[ 1 ];
                thenOps.invoke( cr.getLocalContext() );
            }
        }
        finally
        {
            maybePopRootMap( cr.getBindings() );
        }
    }
}
