package com.brentcroft.tools.jstl;

import com.brentcroft.tools.el.MapBindings;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JstlDocumentTest
{

    @Test
    @Ignore
    public void renderEvents() throws SAXException, ParserConfigurationException, IOException
    {
        DocumentBuilderFactory DBF = DocumentBuilderFactory
                .newInstance();
        DBF.setNamespaceAware( true );

        JstlDocument jstl = new JstlDocument();
        jstl.setContentHandler( new DefaultHandler() );
        jstl.setDocument( DBF
                .newDocumentBuilder()
                .parse( "src/test/resources/templates/jstl/sample-document.xml" ) );

        jstl.getBindings().put( "V_105", new MapBindings().withEntry( "color", "red" ) );

        jstl.renderEvents();

        System.out.println( jstl.getBindings() );
    }

    @Test
    @Ignore
    public void scope_public() throws ParserConfigurationException, IOException, SAXException
    {
        String xml = "<midi xmlns:c='jstl'>" +
                "<c:script>RESOLUTION = 8</c:script>" +
                "<sequence resolution='${ RESOLUTION }'/>" +
                "</midi>";

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();

        documentBuilderFactory.setNamespaceAware( true );

        JstlDocument jstl = new JstlDocument();
        jstl.setContentHandler( new DefaultHandler() );
        jstl.setDocument( documentBuilderFactory
                .newDocumentBuilder()
                .parse( new ByteArrayInputStream( xml.getBytes() ) ) );

        jstl.renderEvents();

        System.out.println( jstl.getBindings() );

        assertEquals( 8, jstl.getBindings().get( "RESOLUTION" ) );
    }
}