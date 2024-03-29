package com.brentcroft.tools.el;

import jakarta.el.ELException;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class ELTemplateManagerTest
{
    private final ELTemplateManager el = new ELTemplateManager();

    @Before
    public void setUp()
    {
        SimpleELContextFactory.clean();
    }

    @Test
    public void test_BasicEL01()
    {
        assertEquals( "", el.expandText( "", null ) );

        assertEquals( "blue-grey", el.expandText( "blue-grey", null ) );
    }

    @Test
    public void test_BasicEL02()
    {
        final String color = "blue-grey";

        assertEquals(
                color,
                el.expandText( "${oof}",
                        new MapBindings()
                                .withEntry( "oof", color ) ) );
    }

    @Test
    public void test_BasicEL03()
    {
        assertEquals(
                "true",
                el.expandText( "${'foo'.matches('foo|bar')}",
                        null ) );

        assertEquals(
                "true",
                el.expandText( "${oof.matches('foo|bar')}",
                        new MapBindings()
                                .withEntry( "oof", "bar" ) ) );
    }

    @Test
    public void test_BasicEL04()
    {
        final String letters = "skjaksjhakjsdhkajsdhkajsdh";

        assertEquals(
                letters.toUpperCase(),
                el.expandText( format( "${'%s'.toUpperCase()}", letters ),
                        null ) );

        assertEquals(
                letters.toUpperCase(),
                el.expandText( "${'skjaksjhakjsdhkajsdhkajsdh'.toUpperCase()}",
                        new MapBindings()
                                .withEntry( "oof", "bar" ) ) );
    }


    @Test
    public void test_BasicELFunctions01()
    {
        // see:
        // http://docs.oracle.com/javase/6/docs/api/java/util/Formatter.html

        assertEquals(
                "20 30 40",
                el.expandText( "${c:format( a, [x, y, z])}",
                        new MapBindings()
                                .withEntry( "a", "%s %s %s" )
                                .withEntry( "x", 20 )
                                .withEntry( "y", 30 )
                                .withEntry( "z", 40 ) ) );


        assertEquals(
                " 40  30  20  10",
                el.expandText( "${c:format( text, [a, b, c, d])}",
                        new MapBindings()
                                .withEntry( "text", "%4$3s %3$3s %2$3s %1$3s" )
                                .withEntry( "a", 10 )
                                .withEntry( "b", 20 )
                                .withEntry( "c", 30 )
                                .withEntry( "d", 40 ) ) );

        assertEquals(
                "Amount gained or lost since last statement: $ (1,750.23)",
                el.expandText( "${c:format( text, [money])}",
                        new MapBindings()
                                .withEntry( "text", "Amount gained or lost since last statement: $ %(,.2f" )
                                .withEntry( "money", - 1750.23 ) ) );


        //el.setValueExpressionFilter( StandardELFilter.XML_ESCAPE_FILTER );

//        assertEquals(
//                "Duke&apos;s Birthday: 05 23,1995",
//                el.expandText( "${c:format( text, date)}",
//                        new MapBindings()
//                                .withEntry( "text", "Duke's Birthday: %1$tm %1$te,%1$tY" )
//                                .withEntry( "date", new GregorianCalendar( 1995, Calendar.MAY, 23 ) ) ) );

        assertEquals(
                "Duke's Birthday: 05 23,1995",
                el.expandText( "${c:format( text, [ date ] )}",
                        new MapBindings()
                                .withEntry( "text", "Duke's Birthday: %1$tm %1$te,%1$tY" )
                                .withEntry( "date", new GregorianCalendar( 1995, Calendar.MAY, 23 ) ) ) );

    }


    @Test
    public void test_system_user_properties()
    {
        assertEquals( System.getProperty( "user.name" ), el.expandText( "${ c:username() }", null ) );
        assertEquals( System.getProperty( "user.home" ), el.expandText( "${ c:userhome() }", null ) );
    }


    @Test
    public void test_DateFormatting()
    {
        assertEquals(
                "2018-08-21",
                el.expandText( "${ c:format( '%1$tY-%1$tm-%1$td', [ c:getTime( '2018-08-21T00:00:00' ) ] ) }", null ) );

        assertEquals(
                new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date() ),
                el.expandText( "${ c:format( '%1$tY-%1$tm-%1$td', [ c:currentTimeMillis() ] ) }", null ) );

        assertEquals(
                new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date() ),
                el.expandText( "${ c:format( '%1$tY-%1$tm-%1$td', [ c:now() ] ) }", null ) );
    }


    @Test
    public void test_ComplexEL()
    {
        long millis = System.currentTimeMillis();

        String expected = "<soap:envelope>\n" +
                "\t<soap:header serial=\"" + millis + "\">alfredo</soap:header>\n" +
                "\t<soap:body>montana</soap:body>\n" +
                "</soap:envelope>";

        String actual = el.expandText(
                "<${tag_envelope}>\n\t<${tag_head} serial=\"${serial}\">${header}</${tag_head}>\n\t<${tag_body}>${request}</${tag_body}>\n</${tag_envelope}>",
                new MapBindings()
                        .withEntry( "header", "alfredo" )
                        .withEntry( "request", "montana" )
                        .withEntry( "serial", millis )

                        .withEntry( "tag_envelope", "soap:envelope" )
                        .withEntry( "tag_body", "soap:body" )
                        .withEntry( "tag_head", "soap:header" ) );

        assertEquals( expected, actual );
    }


//    @Test
//    public void test_XmlEscaping()
//    {
//        //el.setValueExpressionFilter( StandardELFilter.XML_ESCAPE_FILTER );
//
//        final long serial = System.currentTimeMillis();
//
//        final String expected = "<some-tag serial=\"" + serial + "\">montana &amp; arizona: &apos;black&apos; &amp; &quot;blue&quot; (&#65533;20 &gt; $20) <some-tag>";
//
//        final String actual = el.expandText(
//                "<some-tag serial=\"${serial}\">${body}<some-tag>",
//                new MapBindings()
//                        .withEntry( "body", "montana & arizona: 'black' & \"blue\" (�20 > $20) " )
//                        .withEntry( "serial", serial ) );
//
//        assertEquals( expected, actual );
//
//        //System.out.println( actual );
//    }

    @Test
    public void test_XmlDoubleEscaping()
    {
        //el.setValueExpressionFilter( StandardELFilter.XML_UNESCAPE_ESCAPE_FILTER );

        final long serial = System.currentTimeMillis();

        final String expected = "<some-tag serial=\"" + serial + "\">montana &amp; arizona: &apos;black&apos; &amp; &quot;blue&quot; &gt; 20 &#163; <some-tag>";

        final String actual = el.expandText(
                "<some-tag serial=\"${serial}\">${body}<some-tag>",
                new MapBindings()
                        .withEntry( "body", "montana &amp; arizona: &apos;black&apos; &amp; &quot;blue&quot; &gt; 20 &#163; " )
                        .withEntry( "serial", serial ) );

        assertEquals( expected, actual );
    }

    @Test
    public void test_deferred_evaluation()
    {
        String tag = "result";
        MapBindings bindings = new MapBindings()
                .withEntry( "tag", tag );

        /*
            first evaluation
         */

        // expect dollar and trimmed inside curly brackets
        String expected = "result=${x + y + z}";

        String actual = el.expandText(
                // extra space
                "${   tag   }=#{   x + y + z   }",
                bindings
        );

        assertEquals( expected, actual );

        /*
            second evaluation
         */
        bindings
                .withEntry( "x", 3 )
                .withEntry( "y", 4 )
                .withEntry( "z", 5 );

        expected = "result=12";

        actual = el.expandText(
                actual,
                bindings
        );

        assertEquals( expected, actual );
    }


    @Test
    public void test_sort()
    {
        final String expected = "[blue, green, red]";

        final String actual = el.expandText(
                "${ c:sort( collection, comparator ) }",
                new MapBindings()
                        .withEntry( "collection", asList( "red", "green", "blue" ) )
                        .withEntry( "comparator", ( Comparator< String > ) String::compareTo ) );

        assertEquals( expected, actual );
    }

    @Test
    public void test_eval()
    {
        final String expected = "{a=[1, 2, 3], b=[2, 3, 1], c=[3, 1, 2]}";

        final Object actual = el.eval(
                "{ 'a': [1,2,3], 'b': [2,3,1], 'c': [3,1,2] }",
                new MapBindings() );

        assertEquals( expected, actual.toString() );
    }

    @Test
    public void test_escape()
    {
        final String expected = "{a=[1, 2, 3], b=[2, 3, 1], c=[3, 1, 2]}";

        final Object actual = el.expandText(
                "${ { 'a': [1,2,3], 'b': [2,3,1], 'c': [3,1,2] \\} }",
                new MapBindings() );

        assertEquals( expected, actual.toString() );
    }


    @Test
    public void test_lambda()
    {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "color1", "blue" )
                        .withEntry( "color2", "orange" )
                        .withEntry( "color3", "yellow" )
                        .withEntry( "color4", "obtuse" )
                );

        String expression = "colors.entrySet().stream().filter( e -> e.getKey().equals( 'color3' ) ).findFirst().orElse(null).getValue()";

        assertEquals( "yellow", el.eval( expression, bindings ) );
    }

    @Test
    public void test_assignment()
    {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "color1", "blue" )
                        .withEntry( "color2", "orange" )
                        .withEntry( "color3", "yellow" )
                        .withEntry( "color4", "obtuse" )
                );

        el.eval( "colors.color4 = 'red'", bindings );

        assertEquals( "red", el.eval( "colors.color4", bindings ) );
        assertEquals( 11L, el.eval( "$local.time = 10; time + 1", bindings ) );
    }

    @Test
    public void test_import_handler()
    {
        el
                .getELContextFactory()
                .getImportHandler()
                .importClass( SomeClassWithStaticMembers.class.getTypeName() );

        assertEquals( "hello", el.eval( "SomeClassWithStaticMembers.ELSE", new MapBindings() ) );
        assertEquals( 60, el.eval( "SomeClassWithStaticMembers.sixty()", new MapBindings() ) );
    }

    @Test
    public void test_simple_format() {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$xxx", "$local.tryExcept( () -> c:raise( errorText ), (e) -> c:return( c:simpleTrace( e ) ) )" )
                        .withEntry( "$$xxx$args", new MapBindings().withEntry( "errorText", "goodbye" ) )
                );

        try {
            el.eval( "$local.tryExcept( () -> c:raise( 'hello' ), (e) -> c:return( c:simpleTrace( e ) ) )", bindings );
            fail( "Expected ReturnException");
        } catch (ReturnException e) {
            assertEquals( "UserException: hello", e.get() );
        }

        assertEquals( "UserException: goodbye", el.eval( "colors.xxx()", bindings ) );
    }

    @Test
    public void test_steps_args_not_a_map() {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$xxx", "$local.tryExcept( () -> c:raise( errorText ), (e) -> c:return( c:simpleTrace( e ) ) )" )
                        .withEntry( "$$xxx$args", new MapBindings().withEntry( "errorText", null ) )
                );
        String expression = "colors.xxx( 123 )";
        try {
            el.eval( expression, bindings );
            fail("Expected IllegalArgumentException");
        } catch ( ELException e) {
            assertEquals(format("Problem with expression: %s", expression), e.getMessage());
            assertEquals("Steps call must have one argument that is a map, or no argument at all", e.getCause().getMessage());
        }
    }

    @Test
    public void test_steps_args_mandatory() {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$xxx", "$local.tryExcept( () -> c:raise( errorText ), (e) -> c:return( c:simpleTrace( e ) ) )" )
                        .withEntry( "$$xxx$args", new MapBindings().withEntry( "errorText", null ) )
                );

        String expression = "colors.xxx()";

        try {
            el.eval( expression, bindings );
            fail("Expected IllegalArgumentException");
        } catch ( ELException e) {
            assertEquals(format("Problem with expression: %s", expression), e.getMessage());
            assertEquals("Missing mandatory arg: errorText", e.getCause().getMessage());
        }
    }

    @Test
    public void test_steps_args_type_check_01() {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$xxx", "$local.tryExcept( () -> c:raise( errorText ), (e) -> c:return( c:simpleTrace( e ) ) )" )
                        .withEntry( "$$xxx$args", new MapBindings().withEntry( "errorText", "hello world" ) )
                );

        String expression = "colors.xxx( { 'errorText': 1.24 } )";

        try {
            el.eval( expression, bindings );
            fail("Expected IllegalArgumentException");
        } catch ( ELException e) {
            assertEquals(format("Problem with expression: %s", expression), e.getMessage());
            assertEquals("Invalid arg: errorText, expected type String, received type: Double", e.getCause().getMessage());
        }
    }

    @Test
    public void test_steps_args_type_check_02() {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$xxx", "$local.tryExcept( () -> c:raise( errorText ), (e) -> c:return( c:simpleTrace( e ) ) )" )
                        .withEntry( "$$xxx$args", new MapBindings().withEntry( "errorText", "hello world" ) )
                );

        String expression = "colors.xxx( { 'errorText': ( x ) -> c:format( 'x=%s', [ x ] ) } )";

        try {
            el.eval( expression, bindings );
            fail("Expected IllegalArgumentException");
        } catch ( ELException e) {
            assertEquals(format("Problem with expression: %s", expression), e.getMessage());
            assertEquals("Invalid arg: errorText, expected type String, received type: LambdaExpression", e.getCause().getMessage());
        }
    }


    @Test
    public void test_steps_args_type_expect_lambda() {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$xxx", "$local.tryExcept( () -> c:raise( errorText ), (e) -> c:return( c:simpleTrace( e ) ) )" )
                );

        el.eval( "colors.$$xxx$args = { 'errorText': ( x ) -> c:format( 'x=%s', [ x ] ) }", bindings );

        String expression = "colors.xxx( { 'errorText': 17 } )";

        try {
            el.eval( expression, bindings );
            fail("Expected IllegalArgumentException");
        } catch ( ELException e) {
            assertEquals(format("Problem with expression: %s", expression), e.getMessage());
            assertEquals("Invalid arg: errorText, expected type LambdaExpression, received type: Long", e.getCause().getMessage());
        }
    }

    @Test
    public void test_steps_args_lambda_default() {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$xxx", "$local.tryExcept( () -> c:raise( errorText( 'hello' ) ), (e) -> c:return( c:simpleTrace( e ) ) )" )
                );
        el.eval( "colors.$$xxx$args = { 'errorText': ( x ) -> c:format( 'x=%s', [ x ] ) }", bindings );
        assertEquals("UserException: x=hello", el.eval( "colors.xxx()", bindings ));
    }

    @Test
    public void test_steps_args_lambda_override() {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$xxx", "$local.tryExcept( () -> c:raise( errorText( 'hello' ) ), (e) -> c:return( c:simpleTrace( e ) ) )" )
                );
        el.eval( "colors.$$xxx$args = { 'errorText': ( x ) -> c:format( 'x=%s', [ x ] ) }", bindings );

        String expression = "colors.xxx( { 'errorText': ( x ) -> c:format( 'y=%s', [ x ] ) } )";
        assertEquals("UserException: y=hello", el.eval( expression, bindings ));
    }

    @Test
    public void test_throws_exception() {
        MapBindings bindings = new MapBindings();
        String expression = "c:throw( e )";
        try {
            el.eval("$local.e = IllegalArgumentException( 'goodbye cruel world' )", bindings);
            el.eval(expression, bindings);
            fail("Expected IllegalArgumentException");
        } catch ( ELException e) {
            assertEquals("Problems calling function 'c:throw'", e.getMessage());
            assertEquals("goodbye cruel world", e.getCause().getMessage());
        }
    }
}
