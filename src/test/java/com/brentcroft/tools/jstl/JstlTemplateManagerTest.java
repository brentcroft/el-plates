package com.brentcroft.tools.jstl;

//import com.brentcroft.tools.el.StandardELFilter;

import org.junit.Assert;
import org.junit.Test;

public class JstlTemplateManagerTest
{
    private final JstlTemplateManager jstl = new JstlTemplateManager();

    @Test
    public void test_StripComments()
    {
        final String[][] samples = {
                {"<!-- some comment -->", ""},
                {"red <!-- yellow --> lorry", "red  lorry"},
                {"red<!-- 123 -->green<!-- 123 -->blue<!-- 123 -->brown", "redgreenbluebrown"},

                // bad comments not removed
                {"<!-- some comment --", "<!-- some comment --"},
                {"red <!-- yellow -> lorry", "red <!-- yellow -> lorry"},
                {"red<!-- 123 -->green<!- 123 -->blue<!-- 123 -->brown", "redgreen<!- 123 -->bluebrown"},
        };

        for ( String[] sample : samples )
        {
            Assert.assertEquals( sample[ 1 ], jstl.maybeStripComments( sample[ 0 ] ) );
            Assert.assertEquals( sample[ 1 ], jstl.expandText( sample[ 0 ], new MapBindings() ) );
        }
    }

    @Test
    public void readme_test()
    {
        String text = "" +
                "    <c:script>" +
                "        days = [ 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday' ];\n" +
                "        day  = '' + ( java.lang.System.currentTimeMillis() % days.length );\n" +
                "    </c:script>\n" +
                "    generated: ${ c:format( '%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL', c:now() ) }\n" +
                "    <c:choose>\n" +
                "        <c:when test=\"${ days[ day ] eq c:format( '%tA', c:now() ) }\">${ days[ day ] } is special</c:when>\n" +
                "        <c:otherwise>${ c:format( '%tA', c:now() ) } is ordinary (${ days[ day ] } is special)</c:otherwise>\n" +
                "    </c:choose>";

        System.out.println( jstl.expandText( text, new MapBindings() ) );
    }
}
