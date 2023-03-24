package com.brentcroft.tools.jstl;


import com.brentcroft.tools.el.ELTemplateException;
import com.brentcroft.tools.el.MapBindings;
import com.brentcroft.tools.jstl.tag.TagMessages;
import jakarta.el.ELException;
import org.junit.Assert;
import org.junit.Test;


public class JstlCatchTest
{
    private final JstlTemplateManager jstl = new JstlTemplateManager();

    private static final String tag = "catch";

    private static final String attr = "var";

    @Test
    public void test_CATCH_NoAttributeOk()
    {
        final String[] noAttrs = {
                "<c:catch/>",
                "<c:catch>blue</c:catch>"
        };

        for ( String noAttr : noAttrs )
        {
            jstl.expandText( noAttr, new MapBindings().withEntry( "value", true ) );
        }
    }

    @Test
    public void test_CATCH_AttributeBad()
    {
        final String[] badAttrs = {
                "<c:catch test>blue</c:catch>",
                "<c:catch test=>blue</c:catch>",
                "<c:catch test=\">blue</c:catch>",
        };

        for ( String badAttr : badAttrs )
        {
            try
            {
                jstl.expandText( badAttr, new MapBindings() );

                Assert.fail( "Expected an exception" );
            }
            catch ( Exception e )
            {
                Assert.assertEquals( "Parsing Error: Expected closing tag [" + tag + "] but stack is empty", e.getMessage() );
            }
        }
    }

    @Test
    public void test_CATCH_AttributeEmpty()
    {
        final String[] emptyAttrs = {
                "<c:catch var=\"\">blue</c:catch>",
                "<c:catch var=\"\t\n\r\">blue</c:catch>",
                "<c:catch var=\"    \">blue</c:catch>",
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
                Assert.assertEquals( String.format( TagMessages.OPT_ATTR_EMPTY, attr ), e.getMessage() );
            }
        }
    }


    @Test
    public void test_CATCH_ShortCutOk()
    {
        final String[] emptyAttrs = {
                "<c:catch />"
        };

        for ( String emptyAttr : emptyAttrs )
        {
            jstl.expandText( emptyAttr, new MapBindings() );
        }
    }


    @Test
    public void test_CATCH_ValidForms()
    {
        final String[] validForms = {
                "<c:catch var=\"${ value }\">${ result }</c:catch>",
                "<c:catch var=\" \n\n\t\t  ${ value }  \">${ result }</c:catch>",
                "<c:catch var=\"value\">${ result }</c:catch>",
                "<c:catch var=\" \n\n\n  value \n\n\n  \">${ result }</c:catch>",

                // with single quotes
                "<c:catch var='${ value }'>${ result }</c:catch>",
                "<c:catch var=' \n\n\t\t  ${ value }  '>${ result }</c:catch>",
                "<c:catch var='value'>${ result }</c:catch>",
                "<c:catch var=' \n\n\n  value \n\n\n  '>${ result }</c:catch>",


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


    @Test
    public void test_CATCH_ExposesException()
    {
        final String[][] samples = {
                {"<c:catch>${ c:raise( 'whoops' ) }</c:catch>", "caughtException"},
                {"<c:catch var='alfredo'>${ c:raise( 'whoops' ) }</c:catch>", "alfredo"},
                {"<c:catch var='fred bloggs'>${ c:raise( 'whoops' ) }</c:catch>", "fred bloggs"}
        };

        for ( String[] sample : samples )
        {
            final MapBindings fred = new MapBindings();

            jstl.expandText( sample[ 0 ], fred );

            Assert.assertEquals( ELTemplateException.class, fred.get( sample[ 1 ] ).getClass() );
        }
    }
}
