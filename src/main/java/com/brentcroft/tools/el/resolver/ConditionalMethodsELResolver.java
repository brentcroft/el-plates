package com.brentcroft.tools.el.resolver;

import com.brentcroft.tools.el.ELContextFactory;
import com.brentcroft.tools.el.RetriesException;
import com.brentcroft.tools.el.ReturnException;
import com.brentcroft.tools.el.SimpleELContext;
import com.brentcroft.tools.jstl.MapBindings;
import com.sun.el.lang.EvaluationContext;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;

@AllArgsConstructor
public class ConditionalMethodsELResolver extends BaseELResolver
{
    private final ThreadLocal< Stack< MapBindings > > scopeStack;
    private final ELContextFactory elContextFactory;

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

    private void maybePushRootMap( Map< String, Object > rootMap )
    {
        if ( rootMap != null )
        {
            scopeStack.get().push( newContainer( rootMap ) );
        }
    }

    private void maybePopRootMap( Map< String, Object > rootMap )
    {
        if ( rootMap != null )
        {
            scopeStack.get().pop();
        }
    }

    private void tryExcept( ContextAndRoot cr, Object[] params )
    {
        if ( params.length < 2 || ! ( params[ 0 ] instanceof LambdaExpression ) || ! ( params[ 1 ] instanceof LambdaExpression ) )
        {
            throw new ELException( "Must have arguments: tryExcept( LambdaExpression, LambdaExpression )" );
        }
        maybePushRootMap( cr.getRootMap() );
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
            maybePopRootMap( cr.getRootMap() );
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
        maybePushRootMap( cr.getRootMap() );
        try
        {
            long started = System.currentTimeMillis();

            if ( currentLoop >= maxLoops )
            {
                double durationSeconds = Long
                        .valueOf( System.currentTimeMillis() - started ).doubleValue() / 1000;
                onTimeout
                        .orElseThrow( () -> new RetriesException( maxLoops, test.toString() ) )
                        .invoke( cr.getLocalContext(), durationSeconds );
                return;
            }

            while ( returnHandlingTest.apply( cr.getLocalContext(), test ) )
            {
                currentLoop++;
                if ( currentLoop > maxLoops )
                {
                    double durationSeconds = Long
                            .valueOf( System.currentTimeMillis() - started ).doubleValue() / 1000;
                    onTimeout
                            .orElseThrow( () -> new RetriesException( maxLoops, test.toString() ) )
                            .invoke( cr.getLocalContext(), durationSeconds );
                    return;
                }
                ops.invoke( cr.getLocalContext(), currentLoop );
            }
        }
        finally
        {
            maybePopRootMap( cr.getRootMap() );
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
        maybePushRootMap( cr.getRootMap() );
        try
        {
            final LambdaExpression test = ( LambdaExpression ) params[ 0 ];
            if ( returnHandlingTest.apply( cr.getLocalContext(), test ) )
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
            maybePopRootMap( cr.getRootMap() );
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
        maybePushRootMap( cr.getRootMap() );
        try
        {
            final LambdaExpression test = ( LambdaExpression ) params[ 0 ];
            if ( returnHandlingTest.apply( cr.getLocalContext(), test ) )
            {
                final LambdaExpression thenOps = ( LambdaExpression ) params[ 1 ];
                thenOps.invoke( cr.getLocalContext() );
            }
        }
        finally
        {
            maybePopRootMap( cr.getRootMap() );
        }
    }

    @Getter
    public class ContextAndRoot {
        private final ELContext localContext;
        private final Map< String, Object > rootMap;

        public ContextAndRoot(ELContext context, Map< String, Object > baseMap) {
            this.localContext = context;
            this.rootMap = newContainer(  baseMap );
//            if ( context instanceof EvaluationContext )
//            {
//                EvaluationContext ec = ( EvaluationContext ) context;
//                if ( ec.getELContext() instanceof SimpleELContext )
//                {
//                    SimpleELContext selc = ( SimpleELContext ) ec.getELContext();
//                    if ( selc.getRootObjects() != baseMap )
//                    {
//                        this.rootMap = ( Map< String, Object > ) selc.getRootObjects();
//                        this.localContext = new EvaluationContext(
//                            selc.getChildContext( baseMap ),
//                            ec.getFunctionMapper(),
//                            ec.getVariableMapper() );
////                        this.localContext = selc.getChildContext( baseMap );
//                    }
//                }
//            }
        }
    }
}
