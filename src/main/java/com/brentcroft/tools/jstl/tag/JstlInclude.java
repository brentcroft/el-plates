package com.brentcroft.tools.jstl.tag;

import com.brentcroft.tools.jstl.JstlDocument;
import com.brentcroft.tools.jstl.JstlTemplate;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.JstlTemplateManager.JstlTemplateHandler;
import com.brentcroft.tools.jstl.MapBindings;
import lombok.Getter;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Map;

import static com.brentcroft.tools.el.ELTemplateManager.getLocalFileURL;
import static com.brentcroft.tools.jstl.JstlNamespace.prefix;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Getter
public class JstlInclude extends AbstractJstlElement
{
    private final static String TAG = "include";

    private final JstlTemplateManager.JstlTemplateHandler jstlTemplateHandler;

    private static final DocumentBuilderFactory DFB = DocumentBuilderFactory.newInstance();

    static {
        DFB.setNamespaceAware( true );
    }

    private final String uri;
    private final String actualUri;
    private final boolean relative;
    private final boolean recursive;

    public JstlInclude( JstlTemplateHandler jstlTemplateHandler, String uri, boolean relative, boolean recursive )
    {
        this.jstlTemplateHandler = jstlTemplateHandler;

        this.uri = uri;
        this.relative = relative;
        this.recursive = recursive;

        this.actualUri = relative ? jstlTemplateHandler.relativizeUri( uri ) : uri;

        if ( !recursive)
        {
            detectRecursion();
        }

        if (!jstlTemplateHandler.hasRecursiveJstlElement(actualUri) )
        {
            if ( recursive) {
                // must precede further loading
                jstlTemplateHandler.addRecursiveJstlElement(actualUri, this);
            }

            jstlTemplateHandler.loadTemplate( actualUri );
            innerRenderable = new JstlTemplate( this );
        }
    }

    public  String recursionKey()
    {
        return actualUri;
    }

    private void detectRecursion() {

        JstlTemplateHandler parent = jstlTemplateHandler.getParent();

        while ( nonNull(parent) )
        {
            if (actualUri.equals(parent.getUri()))
            {
                throw new RuntimeException( format( TagMessages.INCLUDE_RECURSION, actualUri ) );
            }

            parent = parent.getParent();
        }
    }

    public String render( Map< String, Object > bindings )
    {
        if ( isDeferred() )
        {
            return toText();
        }

        MapBindings localBindings = new MapBindings( bindings );

        if (nonNull(innerRenderable))
        {
            // no output - params applied
            innerRenderable.render(localBindings);
        }

        return jstlTemplateHandler.expandUri( actualUri, localBindings );
    }

    public String toText()
    {
        return String.format( "<%s page=\"%s\" relative=\"%s\" recursive=\"%s\"/>", prefix( TAG ), uri, relative, recursive );
    }

    @Override
    public void emitNodeEvents( Element element, Map< String, Object > bindings, JstlDocument.NodeListEmitter emitter ) throws SAXException
    {
        if ( isDeferred() )
        {
            return;
        }

        String key = "INCLUDES";

        NodeList includes = ( NodeList ) element.getUserData( key );

        if ( isNull( includes ) )
        {
            try
            {
                includes = DFB
                        .newDocumentBuilder()
                        .parse( new InputSource( getLocalFileURL( JstlInclude.class, actualUri ).openStream() ) )
                        .getChildNodes();

                element.setUserData( key, includes, null );
            }
            catch ( ParserConfigurationException | IOException e )
            {
                throw new SAXException( e );
            }
        }

        emitter.emitListEvents( includes, bindings );
    }
}