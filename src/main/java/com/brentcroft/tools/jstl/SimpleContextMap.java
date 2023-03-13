package com.brentcroft.tools.jstl;

import com.brentcroft.tools.el.MapBindings;
import com.brentcroft.tools.el.TextExpander;


/**
 * Supports processing deferred expressions in foreign readers.
 * <p>
 * A foreign reader can use deferred EL expressions
 * to express local context, not visible to the controlling JstlDocument.
 * <p>
 * The JstlDocument will process the deferred expressions so they become
 * undeferred.
 * <p>
 * A foreign reader must intercept and finally render the EL expressions.
 */
public class SimpleContextMap implements ContextValueMapper
{
    private final MapBindings bindings;
    private final TextExpander textExpander;

    public SimpleContextMap( MapBindings bindings, TextExpander textExpander )
    {
        this.bindings = bindings;
        this.textExpander = textExpander;
    }

    @Override
    public ContextValueMapper put( String key, Object value )
    {
        bindings.put( key, value );
        return this;
    }

    @Override
    public ContextValueMapper inContext()
    {
        return new SimpleContextMap( new MapBindings( bindings ), textExpander );
    }

    @Override
    public String map( String key, String value )
    {
        return textExpander.expandText( value, bindings );
    }
}