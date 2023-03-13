package com.brentcroft.tools.jstl;


import com.brentcroft.tools.el.MapBindings;
import com.brentcroft.tools.jstl.tag.TagMessages;
import org.junit.Assert;
import org.junit.Test;


public class JstlIfTest
{
    private final JstlTemplateManager templateManager = new JstlTemplateManager();

    private static final String tag_if = "if";

    private static final String attr_test = "test";

    @Test
    public void testAttributeMissing()
    {
        final String[] templates = {
                "<c:if/>",
                "<c:if>blue</c:if>",
                "<c:if red='yellow'>blue</c:if>"
        };

        for ( String template : templates )
        {
            try
            {
                templateManager.expandText( template, new MapBindings()
                        .withEntry( "value", true ) );

                Assert.fail( "Expected an exception" );
            }
            catch ( Exception e )
            {
                Assert.assertEquals( String.format( TagMessages.REQ_ATTR_MISSING, attr_test ), e.getMessage() );
            }
        }
    }

    @Test
    public void testAttributeBad()
    {
        final String[] badAttrs = {
                "<c:if test>blue</c:if>",
                "<c:if test=>blue</c:if>",
                "<c:if test=\">blue</c:if>",
        };

        for ( String badAttr : badAttrs )
        {
            try
            {
                templateManager.expandText( badAttr, new MapBindings() );

                Assert.fail( "Expected an exception" );
            }
            catch ( Exception e )
            {
                Assert.assertEquals( "Parsing Error: Expected closing tag [" + tag_if + "] but stack is empty",
                        e.getMessage() );
            }
        }
    }

    @Test
    public void testAttributeEmpty()
    {
        final String[] emptyAttrs = {
                "<c:if test=\"\">blue</c:if>",
                "<c:if test=\"\t\n\r\">blue</c:if>",
                "<c:if test=\"    \">blue</c:if>",
        };

        for ( String emptyAttr : emptyAttrs )
        {
            try
            {
                templateManager.expandText( emptyAttr, new MapBindings() );

                Assert.fail( "Expected an exception" );
            }
            catch ( Exception e )
            {
                Assert.assertEquals( String.format( TagMessages.REQ_ATTR_EMPTY, attr_test ), e.getMessage() );
            }
        }
    }


    @Test
    public void testShortCut()
    {
        final String[] emptyAttrs = {
                "<c:if test='red'/>"
        };

        for ( String emptyAttr : emptyAttrs )
        {
            templateManager.expandText( emptyAttr, new MapBindings().withEntry( "red", false ) );
        }
    }


    @Test
    public void testValidForms()
    {
        final String[] validForms = {
                "<c:if test=\"${ value }\">${ result }</c:if>",
                "<c:if test=\" \n\n\t\t  ${ value }  \">${ result }</c:if>",
                "<c:if test=\"value\">${ result }</c:if>",
                "<c:if test=\" \n\n\n  value \n\n\n  \">${ result }</c:if>",

                // with single quotes
                "<c:if test='${ value }'>${ result }</c:if>",
                "<c:if test=' \n\n\t\t  ${ value }  '>${ result }</c:if>",
                "<c:if test='value'>${ result }</c:if>",
                "<c:if test=' \n\n\n  value \n\n\n  '>${ result }</c:if>",


        };


        for ( String validForm : validForms )
        {
            final String expected = "Sunday";
            final String actual = templateManager.expandText(
                    validForm,
                    new MapBindings()
                            .withEntry( "value", true )
                            .withEntry( "result", expected ) );
            Assert.assertEquals( expected, actual );
        }

        for ( String validForm : validForms )
        {
            final String expected = "";
            final String actual = templateManager.expandText(
                    validForm,
                    new MapBindings()
                            .withEntry( "value", false )
                            .withEntry( "result", expected ) );
            Assert.assertEquals( expected, actual );
        }
    }
}
