package com.brentcroft.tools.jstl;


import com.brentcroft.tools.el.*;
import jakarta.el.ELException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;


public class JstlScriptTest
{
    private final JstlTemplateManager jstl = new JstlTemplateManager();


    @Before
    public void clean()
    {
        SimpleELContextFactory.clean();
    }

    @Test()
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
                fail("Expected ELException");
            }
            catch ( ELException e )
            {
                // expected
            }
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
                {"<c:script>$local.fred=2; 0</c:script>${ fred.intValue() }", "2"},
                {"<c:script>$local.fred=2</c:script>${ fred.intValue() }", "2"},

                {"<c:script>$local.fred=8; 0</c:script>${ fred.intValue() }", "8"},
                {"<c:script>$local.fred=8</c:script>${ fred.intValue() }", "8"},

                {"<c:script>$local.fred=2; 0</c:script>${ fred }", "2"},
                {"<c:script>$local.fred=2</c:script>${ fred }", "2"}
        };

        for ( String[] sample : samples )
        {
            final String result = jstl.expandText( sample[ 0 ], new MapBindings() );

            Assert.assertEquals( "[" + sample[ 0 ] + "]", sample[ 1 ], result );
        }
    }
}
