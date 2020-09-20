package com.brentcroft.tools.jstl;

import com.brentcroft.tools.jstl.tag.TagMessages;
import org.junit.Assert;
import org.junit.Test;


public class JstlChooseWhenTest
{
    private final JstlTemplateManager jstl = new JstlTemplateManager();

    private static final String attr = "test";

    @Test
    public void test_CHOOSE_WHEN_AttributeMissing()
    {
        final String[] missingAttrs = {
                "<c:choose><c:when/></c:choose>"
        };

        for ( String missingAttr : missingAttrs )
        {
            try
            {
                jstl.expandText( missingAttr, new MapBindings().withEntry( "value", true ) );

                Assert.fail( "Expected an exception" );
            }
            catch ( Exception e )
            {
                Assert.assertEquals( String.format( TagMessages.REQ_ATTR_MISSING, attr ), e.getMessage() );
            }
        }
    }


    @Test
    public void test_CHOOSE_WHEN_AttributeBad()
    {
        final String[] badAttrs = {
                "<c:choose><c:when test=></c:when></c:choose>",
                "<c:choose><c:when test></c:when></c:choose>",
                "<c:choose><c:when test='></c:when></c:choose>",
        };

        for ( String badAttr : badAttrs )
        {
            try
            {
                jstl.expandText( badAttr, new MapBindings() );

                Assert.fail( "Expected an exception for: [" + badAttr + "]" );
            }
            catch ( Exception e )
            {
                Assert.assertEquals( "Parsing Error: Expected closing tag [when] but stack has: [choose]", e.getMessage() );
            }
        }
    }

    @Test
    public void test_CHOOSE_WHEN_AttributeEmpty()
    {
        final String[] emptyAttrs = {
                "<c:choose><c:when test=\"\"/></c:choose>",
                "<c:choose><c:when test=\"\t\n\r\">blue</c:when></c:choose>",
                "<c:choose><c:when test=\"    \">blue</c:when></c:choose>",
        };

        for ( String emptyAttr : emptyAttrs )
        {
            try
            {
                jstl.expandText( emptyAttr, new MapBindings() );

                Assert.fail( "Expected an exception" );
            }
            catch ( Exception e )
            {
                Assert.assertEquals( String.format( TagMessages.REQ_ATTR_EMPTY, attr ), e.getMessage() );
            }
        }
    }


    @Test
    public void test_CHOOSE_WHEN_ShortCut()
    {
        final String[] emptyAttrs = {
                "<c:choose/>",
                "<c:choose><c:when test='true'/></c:choose>",
                "<c:choose><c:otherwise/></c:choose>",
                "<c:choose><c:when test='false'/><c:otherwise/></c:choose>",
        };

        for ( String emptyAttr : emptyAttrs )
        {
            jstl.expandText( emptyAttr, new MapBindings() );
        }
    }


    @Test
    public void test_CHOOSE_WHEN_ValidForms()
    {
        final String[] validForms = {
                "<c:choose><c:when test=\"${ value }\">${ result }</c:when></c:choose>",
                "<c:choose><c:when test=\" \n\n\t\t  ${ value }  \">${ result }</c:when></c:choose>",
                "<c:choose><c:when test=\"value\">${ result }</c:when></c:choose>",
                "<c:choose><c:when test=\" \n\n\n  value \n\n\n  \">${ result }</c:when></c:choose>",

                // with single quotes
                "<c:choose><c:when test='${ value }'>${ result }</c:when></c:choose>",
                "<c:choose><c:when test=' \n\n\t\t  ${ value }  '>${ result }</c:when></c:choose>",
                "<c:choose><c:when test='value'>${ result }</c:when></c:choose>",
                "<c:choose><c:when test=' \n\n\n  value \n\n\n  '>${ result }</c:when></c:choose>",

                // with otherwise
                "<c:choose><c:when test='${ not value }'/><c:otherwise>${ result }</c:otherwise></c:choose>",
                "<c:choose><c:when test=' \n\n\t\t  ${ !value }  '/><c:otherwise>${ result }</c:otherwise></c:choose>",
                "<c:choose><c:when test='!value'/><c:otherwise>${ result }</c:otherwise></c:choose>",
                "<c:choose><c:when test=' \n\n\n  !value \n\n\n  '/><c:otherwise>${ result }</c:otherwise></c:choose>",

        };


        for ( String validForm : validForms )
        {
            final String expected = "Sunday";
            final String actual = jstl.expandText(
                    validForm,
                    new MapBindings()
                            .withEntry( "value", true )
                            .withEntry( "result", expected ) );
            Assert.assertEquals( expected, actual );
        }

        for ( String validForm : validForms )
        {
            final String expected = "";
            final String actual = jstl.expandText(
                    validForm,
                    new MapBindings()
                            .withEntry( "value", false )
                            .withEntry( "result", expected ) );
            Assert.assertEquals( expected, actual );
        }
    }
}
