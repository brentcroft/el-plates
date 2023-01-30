package com.brentcroft.tools.el;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import jakarta.el.MapELResolver;

import java.util.Map;

public class ConditionalMethodsELResolver extends MapELResolver
{
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
        Object ret = null;

        switch (methodName.toString()) {
            case "ifThen":
                if ( params.length < 2
                        || !(params[0] instanceof LambdaExpression)
                        || !(params[1] instanceof LambdaExpression ) ) {
                    return null;
                }
                ret = ifThen( context, root, params );
                context.setPropertyResolved( base, methodName );
                return ret;

            case "ifThenElse":
                if ( params.length < 3
                        || !(params[0] instanceof LambdaExpression)
                        || !(params[1] instanceof LambdaExpression )
                        || !(params[2] instanceof LambdaExpression)) {
                    return null;
                }
                ret = ifThenElse( context, root, params );
                context.setPropertyResolved( base, methodName );
                return ret;

            case "whileDo":
                if ( params.length < 3
                        || !(params[0] instanceof LambdaExpression)
                        || !(params[1] instanceof LambdaExpression )
                        || !(params[2] instanceof Number)) {
                    return null;
                }
                ret = whileDo( context, root, params );
                context.setPropertyResolved( base, methodName );
                return ret;

            case "tryExcept":
                if ( params.length < 2 || !(params[0] instanceof LambdaExpression) || !(params[1] instanceof LambdaExpression)) {
                    return null;
                }
                ret = tryExcept( context, root, params );
                context.setPropertyResolved( base, methodName );
                return ret;
        }
        return ret;
    }


    private Object tryExcept( ELContext context, Map< String, Object> root, Object[] params )
    {
        if ( params.length < 2 || !(params[0] instanceof LambdaExpression) || !(params[1] instanceof LambdaExpression)) {
            throw new IllegalArgumentException("Must have arguments: tryExcept( LambdaExpression, LambdaExpression )");
        }
        LambdaExpression ops = (LambdaExpression)params[0];
        LambdaExpression onEx = (LambdaExpression)params[1];
        try {
            ops.invoke( context );
        } catch (Exception handled) {
            if (handled.getCause() instanceof ReturnException) {
                throw (ReturnException)handled.getCause();
            }
            Exception cause = handled;
            while (cause.getCause() != null && cause instanceof ELException ) {
                cause = (Exception)cause.getCause();
            }

            System.out.printf( "Handling exception: [%s]: %s%n", cause.getClass().getSimpleName(), cause.getMessage());

            onEx.invoke( context, cause );
        }
        return root;
    }

    private Object whileDo( ELContext context, Map< String, Object> root, Object[] params )
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
        while((Boolean)test.invoke( context ) && maxLoops > currentLoop) {
            currentLoop++;
            ops.invoke( context );
        }
        if (currentLoop >= maxLoops) {
            throw new RetriesException(maxLoops, test.toString());
        }

        return root;
    }


    private Object ifThenElse( ELContext context, Map< String, Object> root, Object[] params )
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

        if((Boolean)test.invoke( context )) {
            thenOps.invoke( context );
        } else {
            elseOps.invoke( context );
        }

        return root;
    }

    private Object ifThen( ELContext context, Map< String, Object> root, Object[] params )
    {
        if ( params.length < 2
                || !(params[0] instanceof LambdaExpression)
                || !(params[1] instanceof LambdaExpression ) ) {
            throw new IllegalArgumentException("Must have arguments: ifThen( LambdaExpression, LambdaExpression )");
        }
        final LambdaExpression test = (LambdaExpression)params[0];
        final LambdaExpression thenOps = (LambdaExpression)params[1];

        if((Boolean)test.invoke( context )) {
            thenOps.invoke( context );
        }

        return root;
    }
}
