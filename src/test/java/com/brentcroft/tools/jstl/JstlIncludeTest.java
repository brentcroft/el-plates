package com.brentcroft.tools.jstl;


import com.brentcroft.tools.jstl.tag.TagMessages;
import org.junit.Assert;
import org.junit.Test;

import static java.lang.String.format;


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
            jstl.expandText( includeAttr, new MapBindings() );
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
            jstl.expandText( includeAttr, new MapBindings() );
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
            jstl.expandText( includeAttr, new MapBindings() );
        }
    }


    @Test
    public void testCircularityThrowsException()
    {
        final String page = "src/test/resources/templates/jstl/test-include-circularity.txt";

        final String[] includeAttrs = {
                "<c:include page='" + page + "'/>",
        };

        try
        {
            for ( String includeAttr : includeAttrs )
            {
                jstl.expandText( includeAttr, new MapBindings() );
            }

            Assert.fail( "Expected Exception" );
        }
        catch ( Exception e )
        {
            Assert.assertEquals( "Circularity", format( TagMessages.INCLUDE_CIRCULARITY, page ), e.getMessage() );
        }
    }


    @Test
    public void testInnerContentThrowsException()
    {
        final String page = "src/test/resources/templates/jstl/test-include.txt";

        final String[] includeAttrs = {
                "<c:include page='" + page + "'>xfy<xfy/></c:include>",
        };

        try
        {
            for ( String includeAttr : includeAttrs )
            {
                jstl.expandText( includeAttr, new MapBindings() );
            }

            Assert.fail( "Expected Exception" );
        }
        catch ( Exception e )
        {
            Assert.assertEquals( "Circularity", format( TagMessages.PARSER_ERROR_UNEXPECTED_TEXT, "include" ), e.getMessage() );
        }
    }
}
