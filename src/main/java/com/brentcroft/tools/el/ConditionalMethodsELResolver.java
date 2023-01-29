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
                ret = ifThen( context, root, params );
                context.setPropertyResolved( base, methodName );
                return ret;

            case "ifThenElse":
                ret = ifThenElse( context, root, params );
                context.setPropertyResolved( base, methodName );
                return ret;

            case "whileDo":
                ret = whileDo( context, root, params );
                context.setPropertyResolved( base, methodName );
                return ret;

            case "tryExcept":
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
                return root;
            }
            Exception cause = handled;
            while (cause.getCause() != null && cause instanceof ELException ) {
                cause = (Exception)cause.getCause();
            }

            System.out.printf( "Handling exception: [%s]: %s%n", cause.getClass().getSimpleName(), cause.getMessage());
            try {
                onEx.invoke( context, cause );
            } catch (RuntimeException re) {
                if (re.getCause() instanceof ReturnException) {
                    return root;
                } else if (re.getCause() != null && re.getClass().isAssignableFrom( re.getCause().getClass() )) {
                    throw (RuntimeException)re.getCause();
                }
                throw re;
            }
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
        try
        {
            while((Boolean)test.invoke( context ) && maxLoops >= 0) {
                maxLoops--;
                ops.invoke( context );
            }
            if (maxLoops < 0) {
                throw new RetriesException(maxLoops, test.toString());
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ReturnException) {
                return ((ReturnException)e.getCause()).get();
            } else if (e.getCause() != null && e.getClass().isAssignableFrom( e.getCause().getClass() )) {
                throw (RuntimeException)e.getCause();
            }
            throw e;
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
        try
        {
            if((Boolean)test.invoke( context )) {
                thenOps.invoke( context );
            } else {
                elseOps.invoke( context );
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ReturnException) {
                return ((ReturnException)e.getCause()).get();
            } else if (e.getCause() != null && e.getClass().isAssignableFrom( e.getCause().getClass() )) {
                throw (RuntimeException)e.getCause();
            }
            throw e;
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
        try
        {
            if((Boolean)test.invoke( context )) {
                thenOps.invoke( context );
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ReturnException) {
                return ((ReturnException)e.getCause()).get();
            } else if (e.getCause() != null && e.getClass().isAssignableFrom( e.getCause().getClass() )) {
                throw (RuntimeException)e.getCause();
            }
            throw e;
        }
        return root;
    }
}
