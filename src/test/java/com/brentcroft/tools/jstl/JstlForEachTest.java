package com.brentcroft.tools.jstl;


import com.brentcroft.tools.jstl.tag.TagMessages;
import org.junit.Assert;
import org.junit.Test;


public class JstlForEachTest
{
    private final JstlTemplateManager jstl = new JstlTemplateManager();

    private final String[] DAYS = new String[]{"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

    private static final String tag = "foreach";


    @Test
    public void test_FOREACH_AttributeMissing()
    {
        final String[] missingAttrs = {
                "<c:foreach/>",
                "<c:foreach>blue</c:foreach>"
        };

        for ( String missingAttr : missingAttrs )
        {
            try
            {
                jstl.expandText( missingAttr, new MapBindings()
                        .withEntry( "value", true ) );

                Assert.fail( "Expected an exception" );
            }
            catch ( Exception e )
            {
                Assert.assertEquals( TagMessages.INCONSISTENT_FOREACH, e.getMessage() );
            }
        }
    }

    @Test
    public void test_FOREACH_AttributeBad()
    {
        final String[] badAttrs = {
                "<c:foreach test>blue</c:foreach>",
                "<c:foreach test=>blue</c:foreach>",
                "<c:foreach test=\">blue</c:foreach>",
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
    public void test_FOREACH_AttributeEmpty()
    {
        final String[] emptyAttrs = {
                "<c:foreach items=''>blue</c:foreach>",
                "<c:foreach items='\t\n\r'>blue</c:foreach>",
                "<c:foreach items='    '>blue</c:foreach>",
                "<c:foreach begin='1'>blue</c:foreach>",
                "<c:foreach end='2'>blue</c:foreach>",
        };

        for ( String emptyAttr : emptyAttrs )
        {
            try
            {
                jstl.expandText( emptyAttr, new MapBindings() );

                Assert.fail( "Expected an exception for : " + emptyAttr );
            }
            catch ( Exception e )
            {
                if ( ! TagMessages.INCONSISTENT_FOREACH.equalsIgnoreCase( e.getMessage() ) )
                {
                    e.printStackTrace();
                }

                Assert.assertEquals( TagMessages.INCONSISTENT_FOREACH, e.getMessage() );
            }
        }
    }


    @Test
    public void test_FOREACH_ShortCut()
    {
        final String[] emptyAttrs = {
                "<c:foreach items='red'/>"
        };

        for ( String emptyAttr : emptyAttrs )
        {
            jstl.expandText( emptyAttr, new MapBindings() );
        }
    }


    @Test
    public void test_FOREACH_ValidForms()
    {
        final String[][] samples = {
                {"<c:foreach items='${ days }'></c:foreach>", ""},
                {"<c:foreach items='${ days }'>x</c:foreach>", "xxxxxxx"},
                {"<c:foreach items='${ days }' var='day'>${ day }</c:foreach>", "mondaytuesdaywednesdaythursdayfridaysaturdaysunday"},
                {"<c:foreach items='${ days }' var='day'>${ day }</c:foreach>", "mondaytuesdaywednesdaythursdayfridaysaturdaysunday"},

                //
                {"<c:foreach begin='0' end='0'>x</c:foreach>", "x"},
                {"<c:foreach begin='0' end='1'>x</c:foreach>", "xx"},
                //{"<c:foreach begin='2' end='4' varStatus='index'>${ days[ index.index ] }</c:foreach>", "wednesdaythursdayfriday"},
                //{"<c:foreach begin='2' end='4' step='2' varStatus='index'>${ days[ index.index ] }</c:foreach>", "wednesdayfriday"},
                //{"<c:foreach begin='${ begin }' end='${ count }' step='${ step }' varStatus='index'>${ days[ index.index ] }</c:foreach>", "wednesdaythursday"},

        };


        for ( String[] sample : samples )
        {
            final String actual = jstl.expandText(
                    sample[ 0 ],
                    new MapBindings()
                            .withEntry( "days", DAYS )
                            .withEntry( "begin", 2 )
                            .withEntry( "count", 3 )
                            .withEntry( "step", 1 ) );
            Assert.assertEquals( sample[ 1 ], actual );
        }

        for ( String[] sample : samples )
        {
            final String actual = jstl.expandText(
                    sample[ 0 ],
                    new MapBindings()
                            .withEntry( "days", DAYS )
                            .withEntry( "begin", 2 )
                            .withEntry( "count", 3 )
                            .withEntry( "step", 1 ) );
            Assert.assertEquals( sample[ 1 ], actual );
        }
    }
}
