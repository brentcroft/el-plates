package com.brentcroft.tools.jstl;


import com.brentcroft.tools.el.Parented;
import lombok.Getter;

import javax.script.Bindings;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Utility hierarchical Map&lt;String, Object&gt; with builder methods for chaining
 * and that implements Bindings.
 *
 * @author ADobson
 */
@Getter
public class MapBindings extends LinkedHashMap< String, Object > implements Bindings, Parented
{
    private static final long serialVersionUID = 8422258558562588221L;

    private Map< String, Object > parent;


    public MapBindings( Map< String, Object > parent )
    {
        this.parent = parent;
    }

    public MapBindings()
    {
        this( null );
    }


    /**
     * Copies all entries from this MapBindings into the specified target.
     * <p>
     * If there is a parent then traverses to parent first:
     * <p>
     * Also note, if parent is a MapBindings then does parent.copyTo( target ), otherwise just copies the parents entries.
     *
     * @param target the target map receiving all entries
     */
    public void copyTo( Map< String, Object > target )
    {
        if ( target == null )
        {
            return;
        }
        else if ( parent != null )
        {
            if ( parent instanceof MapBindings )
            {
                ( ( MapBindings ) parent ).copyTo( target );
            }
            else
            {
                target.putAll( parent );
            }
        }

        target.putAll( this );
    }

    @Override
    public Object get( Object a )
    {
        return super.containsKey( a )
               ? super.get( a )
               : parent != null
                    ? parent.get( a )
                    : null;
    }

    @Override
    public boolean containsKey( Object a )
    {
        return super.containsKey( a ) || parent != null && parent.containsKey( a );
    }

    public MapBindings withParent( Map< String, Object > parent )
    {
        this.parent = parent;
        return this;
    }

    public MapBindings withEntry( String name, Object value )
    {
        this.put( name, value );
        return this;
    }

    public String toString()
    {
        if ( parent == null )
        {
            return super.toString();
        }
        else
        {
            return super.toString() + ( "; " + parent.toString() );
        }
    }
}
