package com.brentcroft.tools.jstl.tag;


import com.brentcroft.tools.jstl.JstlDocument;
import com.brentcroft.tools.jstl.JstlTemplate;
import com.brentcroft.tools.el.MapBindings;
import lombok.extern.java.Log;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.Map;

import static com.brentcroft.tools.jstl.JstlNamespace.prefix;

@Log
public class JstlCatch extends AbstractJstlElement
{
    private final static String TAG = "catch";

    private final String exceptionName;


    public JstlCatch( String exceptionName )
    {
        this.exceptionName = exceptionName;

        innerRenderable = new JstlTemplate( this );
    }

    public String render( Map< String, Object > bindings )
    {
        if ( isDeferred() )
        {
            return toText();
        }

        try
        {
            // protect external bindings from pollution in local scope
            return innerRenderable.render( new MapBindings( bindings ) );

        }
        catch ( Throwable t )
        {
            bindings.put( exceptionName, t );

            log.fine( () -> "Caught exception and inserted as [" + exceptionName + "]: " + t );

            return "";
        }
    }

    public String toText()
    {
        return String.format( "<%s var=\"%s\">%s</%s>", prefix( TAG ), exceptionName, innerRenderable, prefix( TAG ) );
    }

    @Override
    public void emitNodeEvents( Element element, Map< String, Object > bindings, JstlDocument.NodeListEmitter emitter ) throws SAXException
    {
        if ( isDeferred() )
        {
            return;
        }

        try
        {
            emitter.emitListEvents( element.getChildNodes(), bindings );
        }
        catch ( Throwable t )
        {
            bindings.put( exceptionName, t );

            log.fine( () -> "Caught exception and inserted as [" + exceptionName + "]: " + t );
        }
    }
}