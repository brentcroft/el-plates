package com.brentcroft.tools.jstl;

/**
 * A ContextValueMapper allows property values to be mapped to new values with respect to a context.
 * <p>
 * For example, a value may be an EL expression be evaluated, or a key whose value must be hidden.
 */
public interface ContextValueMapper
{
    /**
     * Given a key and value, return a new value
     *
     * @param key   a key
     * @param value a value
     * @return a new value
     */
    String map( String key, String value );

    /**
     * Add an object to the context.
     *
     * @param key   the key to access the object
     * @param value the object
     * @return the ContextValueMapper itself, allowing chained calls
     */
    ContextValueMapper put( String key, Object value );

    /**
     * Provide a new child context, allowing access to parent context objects,
     * but encapsulating any new objects put in the child context
     *
     * @return the ContextValueMapper itself, allowing chained calls
     */
    ContextValueMapper inContext();
}
