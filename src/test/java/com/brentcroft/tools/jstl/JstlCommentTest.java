package com.brentcroft.tools.jstl;


import com.brentcroft.tools.el.MapBindings;
import org.junit.Assert;
import org.junit.Test;


public class JstlCommentTest
{
    private final JstlTemplateManager jstl = new JstlTemplateManager();

    @Test
    public void testComment()
    {
        final String[][] samples = {
                {"blue<c:comment>green</c:comment>red", "blue<!--green-->red"},
                {"<c:comment>green</c:comment>", "<!--green-->"},
                {"xyz<c:comment>\n\ngreen\n\n</c:comment>abc", "xyz<!--\n\ngreen\n\n-->abc"},
        };

        for ( String[] sample : samples )
        {
            Assert.assertEquals( sample[ 1 ], jstl.expandText( sample[ 0 ], new MapBindings() ) );
        }
    }
}
