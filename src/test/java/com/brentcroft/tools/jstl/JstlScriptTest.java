package com.brentcroft.tools.jstl;


import com.brentcroft.tools.jstl.tag.TagMessages;
import org.junit.Assert;
import org.junit.Test;


public class JstlScriptTest
{
    private final JstlTemplateManager jstl = new JstlTemplateManager();

    @Test
    public void test_SCRIPT_InvalidScriptWontCompile()
    {
        final String[] samples = {
                "<c:script>invalid script</c:script>",
        };


        for ( String sample : samples )
        {
            try
            {
                jstl.expandText( sample, new MapBindings() );

                Assert.fail( "Expected exception" );
            }
            catch ( Exception e )
            {
                final String prefix = "javax.script.ScriptException";

                Assert.assertTrue( "Exception message does not start with: [" + prefix + "]", e.getMessage().startsWith( prefix ) );
            }
        }
    }

    @Test
    public void test_SCRIPT_EngineNameNotFound()
    {
        final String[][] samples = {
                {"<c:script engine='arbuthnot'/>", "arbuthnot"},
                {"<c:script engine='green'/>", "green"},
        };


        for ( String[] sample : samples )
        {
            try
            {
                jstl.expandText( sample[ 0 ], new MapBindings() );

                Assert.fail( "Expected exception" );
            }
            catch ( Exception e )
            {
                Assert.assertEquals( String.format( TagMessages.ENGINE_NAME_NOT_FOUND, sample[ 1 ] ), e.getMessage() );
            }
        }
    }


    @Test
    public void test_SCRIPT_Visibility()
    {
        final String[][] samples = {
                {"<c:script>fred=2; 0</c:script>${ fred.intValue() }", "2"},
                {"<c:script>var fred=2</c:script>${ fred.intValue() }", "2"},
                //{ "<c:script>var fred=2</c:script>${ c:format( '%.0f', fred.intValue() ) }", "2" },
                //
                {"<c:script public='true'>fred=8</c:script>${ fred.intValue() }", "8"},
                {"<c:script public='true'>var fred=8</c:script>${ fred.intValue() }", "8"},
                //{ "<c:script public='true'>var fred=8</c:script>${ c:format( '%.0f', fred ) }", "8" },
                {"<c:script public='false'>var fred=2</c:script>${ fred }", ""},
                {"<c:script public='gibberish'>var fred=2</c:script>${ fred }", ""}
        };


        for ( String[] sample : samples )
        {
            final String result = jstl.expandText( sample[ 0 ], new MapBindings() );

            Assert.assertEquals( "[" + sample[ 0 ] + "]", sample[ 1 ], result );
        }
    }

    @Test
    public void test_SCRIPT_RenderOutput()
    {
        final String[][] samples = {
                {"<c:script>25678</c:script>", ""},
                {"<c:script render='true'>25678</c:script>", "25678"},
                {"<c:script render='false'>25678</c:script>", ""},
                {"<c:script render='gibberish'>25678</c:script>", ""}
        };


        for ( String[] sample : samples )
        {
            final String result = jstl.expandText( sample[ 0 ], new MapBindings() );

            Assert.assertEquals( sample[ 1 ], result );
        }
    }


    @Test
    public void test_SCRIPT_Scope()
    {
        final String[][] samples = {
                {"<c:script>fred=2; 0</c:script>${ fred.intValue() }", "2"},
                {"<c:script>var fred=2</c:script>${ fred.intValue() }", "2"},
                //{ "<c:script>var fred=2</c:script>${ c:format( '%.0f', fred.intValue() ) }", "2" },
                //
                {"<c:script public='true'>fred=8</c:script>${ fred.intValue() }", "8"},
                {"<c:script public='true'>var fred=8</c:script>${ fred.intValue() }", "8"},
                //{ "<c:script public='true'>var fred=8</c:script>${ c:format( '%.0f', fred ) }", "8" },
                {"<c:script public='false'>var fred=2</c:script>${ fred }", ""},
                {"<c:script public='gibberish'>var fred=2</c:script>${ fred }", ""}
        };


        for ( String[] sample : samples )
        {
            final String result = jstl.expandText( sample[ 0 ], new MapBindings() );

            Assert.assertEquals( "[" + sample[ 0 ] + "]", sample[ 1 ], result );
        }
    }


}
