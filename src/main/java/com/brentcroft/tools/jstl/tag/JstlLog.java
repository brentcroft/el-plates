package com.brentcroft.tools.jstl.tag;

import com.brentcroft.tools.el.ELTemplateManager;
import com.brentcroft.tools.jstl.JstlDocument;
import com.brentcroft.tools.jstl.JstlTemplate;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;
import lombok.extern.java.Log;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.logging.Level;

import static com.brentcroft.tools.jstl.JstlNamespace.prefix;
import static java.util.Objects.isNull;

@Log
public class JstlLog extends AbstractJstlElement
{
    private final static String TAG = "log";

    private final ELTemplateManager elTemplateManager;

    private final Level level;

    public JstlLog( JstlTemplateManager.JstlTemplateHandler templateHandler, Level level )
    {
        elTemplateManager = templateHandler.getELTemplateManager();
        this.level = level;

        innerRenderable = new JstlTemplate( this );
    }


    public String render( Map< String, Object > bindings )
    {
        if ( isDeferred() )
        {
            return toText();
        }

        log.log( level, () -> innerRenderable.render( new MapBindings( bindings ) ) );

        return "";
    }

    public String toText()
    {
        return String.format( "<%s level=\"%s\">%s</%s>", prefix( TAG ), level, innerRenderable, prefix( TAG ) );
    }

    @Override
    public void emitNodeEvents( Element element, Map< String, Object > bindings, JstlDocument.NodeListEmitter emitter )
    {
        String key = "LOG";

        Object keyObject = element.getUserData( key );

        if ( isNull( keyObject ) )
        {
            innerRenderable.addRenderable( elTemplateManager.buildTemplate( element.getTextContent() ) );

            element.setUserData( key, 1, null );
        }

        render( bindings );
    }
}
