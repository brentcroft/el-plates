package com.brentcroft.tools.el;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.script.Bindings;
import java.util.HashMap;
import java.util.Map;


/**
 * Utility hierarchical Map&lt;String, Object&gt; with builder methods for chaining
 * and that implements Bindings.
 *
 * @author ADobson
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MapBindings extends HashMap< String, Object > implements Bindings
{
    private static final long serialVersionUID = 8422258558562588221L;
    private Map< String, Object > delegate;

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
        else if ( delegate != null )
        {
            if ( delegate instanceof MapBindings )
            {
                ( ( MapBindings ) delegate ).copyTo( target );
            }
            else
            {
                target.putAll( delegate );
            }
        }

        target.putAll( this );
    }

    @Override
    public Object get( Object a )
    {
        return super.containsKey( a )
               ? super.get( a )
               : delegate != null
                 ? delegate.get( a )
                 : null;
    }

    @Override
    public boolean containsKey( Object a )
    {
        return super.containsKey( a ) || delegate != null && delegate.containsKey( a );
    }

    public MapBindings withEntry( String name, Object value )
    {
        this.put( name, value );
        return this;
    }

    public String toString()
    {
        if ( delegate == null )
        {
            return super.toString();
        }
        else
        {
            return super.toString() + ( "; " + delegate.toString() );
        }
    }
}
