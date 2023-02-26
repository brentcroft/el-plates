package com.brentcroft.tools.el;

import com.brentcroft.tools.jstl.MapBindings;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.MapELResolver;

import java.util.Map;

import static java.lang.String.format;

public class MapStepsELResolver extends MapELResolver
{
    private final Evaluator evaluator;
    private final TextExpander expander;
    private final Map< String, Object > staticMap;

    public MapStepsELResolver( TextExpander expander, Evaluator evaluator, Map< String, Object > staticMap )
    {
        if ( expander == null )
        {
            throw new IllegalArgumentException( "Expander must not be null" );
        }
        if ( evaluator == null )
        {
            throw new IllegalArgumentException( "Evaluator must not be null" );
        }
        this.expander = expander;
        this.evaluator = evaluator;
        this.staticMap = staticMap;
    }

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

        @SuppressWarnings( "unchecked" )
        Map< String, Object > root = ( Map< String, Object > ) base;
        Map< String, Object > args = (params != null && params.length > 0 && params[0] instanceof Map )
            ? (Map<String,Object>) params[0]
            : null;

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

        Map< String, Object > scope = newContainer( root );
        scope.put( "$functionName", stepsKey );

        if (args != null ) {
            scope.putAll(args);
        }

        try
        {
            Object[] lastResult = { null };
            Evaluator
                    .stepsStream( steps )
                    .map( step -> expander.expandText( step, scope ) )
                    .forEachOrdered( step -> lastResult[ 0 ] = evaluator.eval( step, scope ) );
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
    }

    public Map< String, Object > newContainer( Map< String, Object > root )
    {
        MapBindings bindings = new MapBindings( root );
        bindings.put( "$local", bindings );
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
