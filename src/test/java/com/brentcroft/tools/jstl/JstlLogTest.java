package com.brentcroft.tools.jstl;

import com.brentcroft.tools.el.MapBindings;
import org.junit.Test;


public class JstlLogTest
{
    private final JstlTemplateManager jstl = new JstlTemplateManager();

    @Test
    public void testLevelAttributes()
    {
        final String[] templates = {
                "<c:log level='info'>this is info</c:log>",
                "<c:log level='fine'>this is debug</c:log>",
                "<c:log level='fatal'>this is fatal</c:log>",
                "<c:log>this is default</c:log>",
        };

        for ( String template : templates )
        {
            jstl.expandText( template, new MapBindings() );
        }
    }


    @Test
    public void testOutputVariables()
    {
        final String[] templates = {
                "<c:log level='info'>alfredo ${ alfredo }</c:log>",
        };

        for ( String template : templates )
        {
            jstl.expandText( template, new MapBindings()
                    .withEntry( "alfredo", "montana" ) );
        }
    }
}
