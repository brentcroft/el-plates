package com.brentcroft.tools.jstl;

import org.w3c.dom.Node;

public class JstlNamespace
{
    public static final String PREFIX = "c";

    public static final String URI = "jstl";


    public static String prefix( String name )
    {
        return PREFIX + ":" + name;
    }

    public static boolean isNamespace( Node node )
    {
        return URI.equals( node.getNamespaceURI() );
    }
}
