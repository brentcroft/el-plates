package com.brentcroft.tools.jstl.tag;


import com.brentcroft.tools.jstl.JstlDocument;
import com.brentcroft.tools.jstl.JstlTemplate;
import com.brentcroft.tools.el.MapBindings;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.Map;

import static com.brentcroft.tools.jstl.JstlNamespace.prefix;

public class JstlComment extends AbstractJstlElement
{
    private final static String TAG = "comment";

    public JstlComment()
    {
        innerRenderable = new JstlTemplate( this );
    }

    public String render( Map< String, Object > bindings )
    {
        // protect external bindings from pollution in local scope
        return "<!--" + innerRenderable.render( new MapBindings( bindings ) ) + "-->";
    }

    public String toText()
    {
        return String.format( "<%s>%s</%s>", prefix( TAG ), innerRenderable, prefix( TAG ) );
    }

    @Override
    public void emitNodeEvents( Element element, Map< String, Object > bindings, JstlDocument.NodeListEmitter emitter ) throws SAXException
    {
        // no-op
    }

}