package com.brentcroft.tools.jstl;

import com.brentcroft.tools.el.MapBindings;
import com.brentcroft.tools.jstl.tag.JstlElement;
import lombok.Getter;
import lombok.Setter;
import org.w3c.dom.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

@Getter
@Setter
public class JstlDocument
{
    private final MapBindings bindings = new MapBindings();
    private final JstlTemplateManager jstlTemplateManager = new JstlTemplateManager();

    private JstlTemplateManager.JstlTemplateHandler templateHandler;

    private Document document;
    private ContentHandler contentHandler;


    public void renderEvents() throws SAXException
    {
        if ( isNull( document ) )
        {
            throw new RuntimeException( "No document." );
        }

        if ( isNull( contentHandler ) )
        {
            throw new RuntimeException( "No contentHandler." );
        }

        setTemplateHandler( jstlTemplateManager.new JstlTemplateHandler( document.getDocumentURI(), null ) );

        emitChildren( document.getChildNodes(), bindings );
    }

    public interface NodeListEmitter
    {
        void emitListEvents( NodeList nodeList, Map< String, Object > bindings ) throws SAXException;
    }

    private static List< Node > getChildNodes( NodeList parent )
    {
        return isNull( parent )
               ? Collections.emptyList()
               : IntStream
                       .range( 0, parent.getLength() )
                       .mapToObj( parent::item )
                       .collect( Collectors.toList() );
    }

    private boolean isJstlElement( Element element )
    {
        return JstlNamespace.isNamespace( element );
    }


    private void emitChildren( NodeList parent, Map< String, Object > bindings ) throws SAXException
    {
        for ( Node node : getChildNodes( parent ) )
        {
            switch ( node.getNodeType() )
            {
                case Node.TEXT_NODE:
                    final Text text = ( Text ) node;
                    char[] chars = jstlTemplateManager.expandText( text.getWholeText(), bindings ).toCharArray();
                    contentHandler.characters( chars, 0, chars.length );
                    break;

                case Node.ELEMENT_NODE:
                    final Element element = ( Element ) node;
                    NamedNodeMap elementAttributes = element.getAttributes();

                    if ( isJstlElement( element ) )
                    {
                        String tag = element.getTagName().substring( 2 );

                        final JstlTag jstlType = JstlTag.valueOf( tag.toUpperCase() );

                        JstlElement jstlElement = ( JstlElement ) element.getUserData( jstlType.name() );

                        if ( isNull( jstlElement ) )
                        {
                            Map< String, String > ai = new HashMap<>();

                            ofNullable( elementAttributes )
                                    .ifPresent( attrs -> IntStream
                                            .range( 0, attrs.getLength() )
                                            .mapToObj( i -> ( Attr ) attrs.item( i ) )
                                            .forEach( attr -> ai.put( attr.getName(), attr.getValue() ) ) );

                            jstlElement = jstlType.newJstlElement( templateHandler, ai );
                            jstlElement.normalize();
                            element.setUserData( jstlType.name(), jstlElement, null );
                        }


                        jstlElement.emitNodeEvents( element, bindings, this::emitChildren );
                    }
                    else
                    {
                        AttributesImpl attributes = new AttributesImpl();

                        for ( int i = 0, n = element.getAttributes().getLength(); i < n; i++ )
                        {
                            Attr attr = ( Attr ) elementAttributes.item( i );
                            attributes.addAttribute(
                                    attr.getNamespaceURI(),
                                    attr.getLocalName(),
                                    attr.getNodeName(),
                                    "",
                                    getJstlTemplateManager()
                                            .expandText( attr.getValue(), bindings ) );
                        }


                        contentHandler.startElement(
                                element.getNamespaceURI(),
                                element.getLocalName(),
                                element.getTagName(),
                                attributes
                        );

                        emitChildren( element.getChildNodes(), bindings );

                        contentHandler.endElement(
                                element.getNamespaceURI(),
                                element.getLocalName(),
                                element.getTagName()
                        );
                    }

                    break;
            }
        }
    }
}
