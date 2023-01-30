package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import jakarta.el.MapELResolver;

import java.util.Map;
import java.util.function.BiFunction;

public class ConditionalMethodsELResolver extends MapELResolver
{
    BiFunction<ELContext,LambdaExpression,Boolean> returnHandlingTest = ( ELContext context, LambdaExpression test) -> {
        try {
            return (Boolean)test.invoke( context );
        } catch (ReturnException e) {
            return Boolean.parseBoolean( e.get().toString() );
        } catch (ELException e) {
            ELException cause = e;
            while (cause.getCause() != null && cause.getCause() instanceof ELException ) {
                cause = (ELException)cause.getCause();
            }
            throw cause;
        }
    };


    @Override
    public Object getValue( ELContext context, Object base, Object property )
    {
        return null;
    }

    public Object invoke(ELContext context, Object base, Object methodName, Class<?>[] paramTypes, Object[] params) {
        if (base == null || methodName == null) {
            return null;
        }
        if (!(base instanceof Map)) {
            return null;
        }
        Map<String, Object> root = (Map<String,Object>)base;

        switch (methodName.toString()) {
            case "ifThen":
                if ( params.length < 2
                        || !(params[0] instanceof LambdaExpression)
                        || !(params[1] instanceof LambdaExpression ) ) {
                    return null;
                }
                ifThen( context, root, params );
                context.setPropertyResolved( base, methodName );
                return base;

            case "ifThenElse":
                if ( params.length < 3
                        || !(params[0] instanceof LambdaExpression)
                        || !(params[1] instanceof LambdaExpression )
                        || !(params[2] instanceof LambdaExpression)) {
                    return null;
                }
                ifThenElse( context, root, params );
                context.setPropertyResolved( base, methodName );
                return base;

            case "whileDo":
                if ( params.length < 3
                        || !(params[0] instanceof LambdaExpression)
                        || !(params[1] instanceof LambdaExpression )
                        || !(params[2] instanceof Number)) {
                    return null;
                }
                whileDo( context, root, params );
                context.setPropertyResolved( base, methodName );
                return base;

            case "tryExcept":
                if ( params.length < 2 || !(params[0] instanceof LambdaExpression) || !(params[1] instanceof LambdaExpression)) {
                    return null;
                }
                tryExcept( context, root, params );
                context.setPropertyResolved( base, methodName );
                return base;
        }
        return null;
    }


    private void tryExcept( ELContext context, Map< String, Object> root, Object[] params )
    {
        if ( params.length < 2 || !(params[0] instanceof LambdaExpression) || !(params[1] instanceof LambdaExpression)) {
            throw new IllegalArgumentException("Must have arguments: tryExcept( LambdaExpression, LambdaExpression )");
        }
        LambdaExpression ops = (LambdaExpression)params[0];
        LambdaExpression onEx = (LambdaExpression)params[1];
        try {
            ops.invoke( context );
        } catch (ReturnException e) {
            throw e;
        } catch (Exception handled) {
            Exception cause = handled;
            while (cause.getCause() != null && cause instanceof ELException ) {
                cause = (Exception)cause.getCause();
            }
            if (cause.getCause() instanceof ReturnException) {
                throw (ReturnException)cause.getCause();
            }

            onEx.invoke( context, cause );
        }
    }

    private void whileDo( ELContext context, Map< String, Object> root, Object[] params )
    {
        if ( params.length < 3
                || !(params[0] instanceof LambdaExpression)
                || !(params[1] instanceof LambdaExpression )
                || !(params[2] instanceof Number)) {
            throw new IllegalArgumentException("Must have arguments: whileDo( LambdaExpression, LambdaExpression, Number )");
        }
        final LambdaExpression test = (LambdaExpression)params[0];
        final LambdaExpression ops = (LambdaExpression)params[1];
        int maxLoops = ((Number)params[2]).intValue();
        int currentLoop = 0;

        while(returnHandlingTest.apply( context, test ) && maxLoops > currentLoop) {
            currentLoop++;
            ops.invoke( context );
        }
        if (currentLoop >= maxLoops) {
            throw new RetriesException(maxLoops, test.toString());
        }
    }


    private void ifThenElse( ELContext context, Map< String, Object> root, Object[] params )
    {
        if ( params.length < 3
                || !(params[0] instanceof LambdaExpression)
                || !(params[1] instanceof LambdaExpression )
                || !(params[2] instanceof LambdaExpression)) {
            throw new IllegalArgumentException("Must have arguments: ifThenElse( LambdaExpression, LambdaExpression, LambdaExpression )");
        }
        final LambdaExpression test = (LambdaExpression)params[0];
        final LambdaExpression thenOps = (LambdaExpression)params[1];
        final LambdaExpression elseOps = (LambdaExpression)params[2];

        if(returnHandlingTest.apply( context, test )) {
            thenOps.invoke( context );
        } else {
            elseOps.invoke( context );
        }
    }

    private void ifThen( ELContext context, Map< String, Object> root, Object[] params )
    {
        if ( params.length < 2
                || !(params[0] instanceof LambdaExpression)
                || !(params[1] instanceof LambdaExpression ) ) {
            throw new IllegalArgumentException("Must have arguments: ifThen( LambdaExpression, LambdaExpression )");
        }
        final LambdaExpression test = (LambdaExpression)params[0];
        final LambdaExpression thenOps = (LambdaExpression)params[1];

        if(returnHandlingTest.apply( context, test )) {
            thenOps.invoke( context );
        }
    }
}
