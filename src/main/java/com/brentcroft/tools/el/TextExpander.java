package com.brentcroft.tools.el;

import java.util.Map;

public interface TextExpander
{
    String expandText( String text, Map< String, Object > context );
}
