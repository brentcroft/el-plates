package com.brentcroft.tools.el;

import com.brentcroft.tools.jstl.MapBindings;
import org.junit.Ignore;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;


public class ELTemplateManagerTest
{
    private final ELTemplateManager el = new ELTemplateManager();


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


        el.setValueExpressionFilter( null );

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

        //System.out.println( actual );
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

        System.out.println( actual );
    }


    @Test
    public void test_eval()
    {
        final String expected = "{a=[1, 2, 3], b=[2, 3, 1], c=[3, 1, 2]}";

        final Object actual = el.eval(
                "{ 'a': [1,2,3], 'b': [2,3,1], 'c': [3,1,2] }",
                new MapBindings() );

        assertEquals( expected, actual.toString() );

        System.out.println( actual );
    }

    @Test
    public void test_escape()
    {
        final String expected = "{a=[1, 2, 3], b=[2, 3, 1], c=[3, 1, 2]}";

        final Object actual = el.expandText(
                "${ { 'a': [1,2,3], 'b': [2,3,1], 'c': [3,1,2] \\} }",
                new MapBindings() );

        assertEquals( expected, actual.toString() );

        System.out.println( actual );
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

        el.eval("colors.color4 = 'red'", bindings );

        assertEquals( "red", el.expandText( "${ colors.color4 }", bindings ) );
    }

    @Test
    public void test_return()
    {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "color1", "blue" )
                        .withEntry( "color2", "orange" )
                        .withEntry( "color3", "yellow" )
                        .withEntry( "color4", "obtuse" )
                );

        String expression = "colors.last = 23; c:return( 29 ); colors.last = 25";

        assertEquals( 29L, el.eval( expression, bindings ) );
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
    public void test_thread_local_resolver() {
        Stack< Map<String,Object> > stack = new Stack<>();

        ThreadLocal< Stack< Map<String,Object> > > scopeStack = ThreadLocal.withInitial( () -> stack );
        ThreadLocalStackELResolver tlsELResolver = new ThreadLocalStackELResolver(el,el, scopeStack);
        el.addPrimaryResolvers( tlsELResolver );

        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "color1", "blue" )
                        .withEntry( "color2", "orange" )
                        .withEntry( "color3", "yellow" )
                        .withEntry( "color4", "obtuse" )
                );

        stack.push(scope);

        try
        {
            String expression = "colors.entrySet().stream().filter( e -> e.getKey().equals( 'color3' ) ).findFirst().orElse(null).getValue()";
            assertEquals( "yellow", el.eval( expression, new HashMap<>() ) );
        }
        finally
        {
            stack.pop();
        }
    }


    @Test
    public void test_thread_local_resolver_steps() {
        Stack< Map<String,Object> > stack = new Stack<>();
        stack.push( new HashMap<>() );

        ThreadLocal< Stack< Map<String,Object> > > scopeStack = ThreadLocal.withInitial( () -> stack );
        ThreadLocalStackELResolver tlsELResolver = new ThreadLocalStackELResolver(el,el, scopeStack);
        el.addPrimaryResolvers( tlsELResolver );

        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "x", 1 )
                        .withEntry( "y", 2 )
                        .withEntry( "$$helloWorld", "c:println('hello world '.concat($self.plus()));\n# comments \n $self.minus({'x':100,'y':37})" )
                        .withEntry( "$$plus", "x + y" )
                        .withEntry( "$$minus", "x - y" ));

        assertEquals( 63L, el.eval( "colors.helloWorld()", scope ) );
    }


    @Test
    public void test_static_resolver() {
        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "color1", "blue" )
                        .withEntry( "color2", "orange" )
                        .withEntry( "color3", "yellow" )
                        .withEntry( "color4", "obtuse" )
                );

        SimpleELResolver staticResolver = new SimpleELResolver( scope );

        el.addSecondaryResolvers( staticResolver );

        String expression = "colors.entrySet().stream().filter( e -> e.getKey().equals( 'color3' ) ).findFirst().orElse(null).getValue()";

        assertEquals( "yellow", el.eval( expression, new HashMap<>() ) );
    }


    @Test
    public void test_scope_precedence() {

        Stack< Map<String,Object> > stack = new Stack<>();

        ThreadLocal< Stack< Map<String,Object> > > scopeStack = ThreadLocal.withInitial( () -> stack );
        ThreadLocalStackELResolver tlsELResolver = new ThreadLocalStackELResolver(el,el, scopeStack);
        el.addPrimaryResolvers( tlsELResolver );

        MapBindings stackScope = new MapBindings()
                .withEntry( "color1", "blue" );
        MapBindings modelScope = new MapBindings()
                .withEntry( "color1", "yellow" )
                .withEntry( "color2", "orange" );
        MapBindings staticScope = new MapBindings()
                .withEntry( "color1", "green" )
                .withEntry( "color2", "blue" )
                .withEntry( "color3", "red" );

        SimpleELResolver staticResolver = new SimpleELResolver( staticScope );

        el.addSecondaryResolvers( staticResolver );

        stack.push(stackScope);

        try
        {
            assertEquals( "blue", el.eval( "color1", modelScope) );
            assertEquals( "orange", el.eval( "color2", modelScope) );
            assertEquals( "red", el.eval( "color3", modelScope) );
        }
        finally
        {
            stack.pop();
        }
    }

}
