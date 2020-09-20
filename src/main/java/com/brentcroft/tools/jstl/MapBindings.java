package com.brentcroft.tools.jstl;


import com.brentcroft.tools.el.ELTemplateException;
import com.brentcroft.tools.el.ELTemplateManager;
import com.brentcroft.tools.el.TextExpander;

import javax.script.Bindings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;


/**
 * Utility hierarchical Map&lt;String, Object&gt; with builder methods for chaining
 * and that implements Bindings.
 *
 * @author ADobson
 */
public class MapBindings extends LinkedHashMap< String, Object > implements Bindings
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

    public Map< String, Object > getParent()
    {
        return parent;
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
        return super.containsKey( a ) ? super.get( a ) : parent != null && parent.containsKey( a ) ? parent.get( a ) : null;
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


    /**
     * Renders all values that are EL Templates.
     * <p>
     * Since templates may contain references to other entries,
     * and such entries may be templates themselves,
     * it's  necessary to detect entry values with templates, render them,
     * and detect and render again, until no EL Templates remain,
     * or more than 100 attempts have been made,
     * when a RuntimeException is thrown.
     * <p>
     * Any templates that fail to compile are indicated in the exception message.
     *
     * @param textExpander a TextExpander to implement the rendering
     */
    public void renderAllTemplates( TextExpander textExpander )
    {
        List< ELTemplateException > ignoredExceptions = new ArrayList<>();

        boolean changed;

        int loopIndex = 0;
        do
        {
            loopIndex++;

            if ( loopIndex > 100 )
            {
                throw new RuntimeException(
                        format( "Too many loops [100] while expanding properties: Is there a circular definition?; %s %s",
                                this,
                                ignoredExceptions.isEmpty()
                                ? ""
                                : ignoredExceptions
                        ) );
            }

            // set false at beginning of each run
            changed = false;

            // clear for action
            ignoredExceptions.clear();


            for ( String key : keySet() )
            {
                final Object value = get( key );

                // and therefore not null
                if ( value instanceof String )
                {
                    String oldValue = ( String ) value;

                    // if not a template then doesn't need expanding
                    if ( ELTemplateManager.hasTemplate( oldValue ) )
                    {
                        try
                        {
                            String newValue = textExpander.expandText( ( String ) value, this );

                            // if has changed value then loop
                            if ( ! oldValue.equals( newValue ) )
                            {
                                put( key, newValue );

                                changed = true;
                            }
                            // if hasn't changed value but is still a template
                            else if ( ELTemplateManager.hasTemplate( newValue ) )
                            {
                                throw new RuntimeException( format( "Detected stationary EL expression (i.e. circularity [%s]): key=[%s], value=[%s]:\n %s",
                                        loopIndex,
                                        key,
                                        newValue,
                                        this ) );
                            }
                        }
                        catch ( ELTemplateException elte )
                        {
                            // try again later
                            ignoredExceptions.add( elte );
                        }
                    }
                }
            }
        } while ( changed );

        if ( ! ignoredExceptions.isEmpty() )
        {
            throw new RuntimeException(
                    format( "Some entries generated EL Template exceptions:%n bindings=%s%n%s",
                            this,
                            ignoredExceptions
                                    .stream()
                                    .map( Throwable::getMessage )
                                    .collect( Collectors.joining( "\n " ) )
                    ) );
        }
    }

}
