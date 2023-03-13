package com.brentcroft.tools.jstl;


import com.brentcroft.tools.el.MapBindings;
import com.brentcroft.tools.jstl.tag.TagMessages;
import org.junit.Assert;
import org.junit.Test;

import static java.lang.String.format;
import static java.util.Objects.nonNull;


public class JstlIncludeTest
{
    private final JstlTemplateManager jstl = new JstlTemplateManager();

    private static final String attr = "page";

    @Test
    public void testMissingAttribute()
    {
        final String[] includeAttrs = {
                "<c:include/>",
                "<c:include xpage='src/test/resources/templates/jstl/test-include.txt'/>",
        };

        for ( String includeAttr : includeAttrs )
        {
            try
            {
                jstl.expandText( includeAttr, new MapBindings() );

                Assert.fail( "Expected an exception" );
            }
            catch ( Exception e )
            {
                Assert.assertEquals( format( TagMessages.REQ_ATTR_MISSING, attr ), e.getMessage() );
            }
        }
    }


    @Test
    public void testSimple()
    {
        final String[] includeAttrs = {
                "<c:include page='src/test/resources/templates/jstl/test-include.txt'/>",
        };

        for ( String includeAttr : includeAttrs )
        {
            jstl.expandText( includeAttr, new MapBindings().withEntry( "fred", "bloggs" ) );
        }
    }

    @Test
    public void testParent()
    {
        final String[] includeAttrs = {
                "<c:include page='src/test/resources/templates/jstl/test-include-parent.txt'/>",
        };

        for ( String includeAttr : includeAttrs )
        {
            jstl.expandText( includeAttr, new MapBindings().withEntry( "fred", "bloggs" ) );
        }
    }


    @Test
    public void testGrandParent()
    {
        final String[] includeAttrs = {
                "<c:include page='src/test/resources/templates/jstl/test-include-grandparent.txt'/>",
        };

        for ( String includeAttr : includeAttrs )
        {
            jstl.expandText( includeAttr, new MapBindings().withEntry( "fred", "bloggs" ) );
        }
    }


    @Test
    public void testCircularityThrowsException()
    {
        final String page = "src/test/resources/templates/jstl/test-include-circularity.txt";

        final String[] includeAttrs = {
                "<c:include page='" + page + "' recursive='false'/>",
        };

        Exception unexpectedException = null;
        
        try
        {
            for ( String includeAttr : includeAttrs )
            {
                jstl.expandText(
                    includeAttr,
                    new MapBindings()
                );
            }

            Assert.fail( "Expected Exception" );
        }
        catch ( Exception e )
        {
            unexpectedException = e;

            Assert.assertEquals( "Recursion", format( TagMessages.INCLUDE_RECURSION, page), e.getMessage() );

            unexpectedException = null;
        }
        finally
        {
            if (nonNull(unexpectedException))
            {
                unexpectedException.printStackTrace();
            }
        }
    }


    @Test
    public void testParams()
    {
        final String page = "src/test/resources/templates/jstl/test-include.txt";

        final String[] includeAttrs = {
                "<c:include page='" + page + "'><c:param name='fred' value='${ surname }'/></c:include>",
        };

        MapBindings bindings = new MapBindings();

        bindings.put("surname", "bloggs");

        try
        {
            for ( String includeAttr : includeAttrs )
            {
                jstl.expandText( includeAttr, bindings );

                //assertTrue(bindings.isEmpty());
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
}
