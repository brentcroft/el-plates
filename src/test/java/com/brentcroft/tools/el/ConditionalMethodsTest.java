package com.brentcroft.tools.el;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;


public class ConditionalMethodsTest
{
    private final ELTemplateManager el = new ELTemplateManager();

    @Before
    public void setCurrentDirectory()
    {
        SimpleELContextFactory.clean();
    }

    @Test
    public void test_thread_local_resolver()
    {
        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "color1", "blue" )
                        .withEntry( "color2", "orange" )
                        .withEntry( "color3", "yellow" )
                        .withEntry( "color4", "obtuse" )
                );

        el.getELContextFactory().getScopeStack().get().push( scope );

        try
        {
            String expression = "colors.entrySet().stream().filter( e -> e.getKey().equals( 'color3' ) ).findFirst().orElse(null).getValue()";
            assertEquals( "yellow", el.eval( expression, new HashMap<>() ) );
        }
        finally
        {
            el.getELContextFactory().getScopeStack().get().pop();
        }
    }

    @Test
    public void test_return()
    {
        MapBindings bindings = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$testReturn", "c:return( 29 ); 31" )
                        .withEntry( "$$testReturnInIfThen", "$self.ifThen( () -> c:return(true), () -> c:return('ok') );" )
                        .withEntry( "$$testReturnInIfThenElse", "$self.ifThenElse( () -> c:return(false), () -> c:return('XXX'), () -> c:return('ok') );" )
                        .withEntry( "$$testReturnInWhileDo", "$self.called = 'no'; $self.whileDo( () -> c:return(false), () -> ( $self.called = 'yes' ), 5 ); called" )
                );

        assertEquals( 29L, el.eval( "colors.testReturn()", bindings ) );
        assertEquals( "ok", el.eval( "colors.testReturnInIfThen()", bindings ) );
        assertEquals( "ok", el.eval( "colors.testReturnInIfThenElse()", bindings ) );
        assertEquals( "no", el.eval( "colors.testReturnInWhileDo()", bindings ) );
    }



    @Test
    public void args_resolved_before_model_and_local()
    {
        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "x", 1 )
                        .withEntry( "y", 2 )
                        .withEntry( "$$helloWorld", "$self.minus({'x':100,'y':37})" )
                        .withEntry( "$$minus", "x - y" ) );

        el.eval( "$static.x = 37; $static.y = 23", scope );
        el.eval( "$local.x = 17; $local.y = 13", scope );

        assertEquals( 63L, el.eval( "colors.helloWorld()", scope ) );
    }

    @Test
    public void model_resolved_before_local()
    {
        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "x", 5 )
                        .withEntry( "y", 2 )
                        .withEntry( "$$helloWorld","$self.minus()" )
                        .withEntry( "$$minus", "x - y" ) );

        el.eval( "$static.x = 37; $static.y = 23", scope );
        el.eval( "$local.x = 17; $local.y = 13", scope );

        assertEquals( 3L, el.eval( "colors.helloWorld()", scope ) );
    }

    @Test
    public void local_resolved_before_static()
    {
        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$helloWorld","$self.minus()" )
                        .withEntry( "$$minus", "x - y" ) );

        el.eval( "$static.x = 37; $static.y = 23", scope );
        el.eval( "$local.x = 17; $local.y = 13", scope );
        assertEquals( 4L, el.eval( "colors.helloWorld()", scope ) );
    }

    @Test
    public void static_resolved()
    {
        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "$$helloWorld","$self.minus()" )
                        .withEntry( "$$minus", "x - y" ) );

        el.eval( "$static.x = 37; $static.y = 23", scope );
        assertEquals( 14L, el.eval( "colors.helloWorld()", scope ) );
    }




    @Test
    public void test_static_resolver()
    {
        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "color1", "blue" )
                        .withEntry( "color2", "orange" )
                        .withEntry( "color3", "yellow" )
                        .withEntry( "color4", "obtuse" )
                );

        el.getELContextFactory().getScopeStack().get().push( scope );

        String expression = "colors.entrySet().stream().filter( e -> e.getKey().equals( 'color3' ) ).findFirst().orElse(null).getValue()";

        assertEquals( "yellow", el.eval( expression, new HashMap<>() ) );
    }


    @Test
    public void test_scope_precedence()
    {
        MapBindings staticScope = new MapBindings()
                .withEntry( "color1", "green" )
                .withEntry( "color2", "blue" )
                .withEntry( "color3", "red" );

        el.getELContextFactory().getStaticModel().putAll( staticScope );

        MapBindings stackScope = new MapBindings()
                .withEntry( "color1", "blue" )
                .withEntry( "color2", "gold" );

        el.getELContextFactory().getScopeStack().get().push( stackScope );

        MapBindings modelScope = new MapBindings()
                .withEntry( "color1", "yellow" )
                .withEntry( "level2", new MapBindings()
                        .withEntry( "color1", "puce" )
                        .withEntry( "color2", "purple" )
                );

        assertEquals( "yellow", el.eval( "color1", modelScope ) );
        assertEquals( "gold", el.eval( "color2", modelScope ) );
        assertEquals( "red", el.eval( "color3", modelScope ) );
        assertEquals( "puce", el.eval( "level2.color1", modelScope ) );
        assertEquals( "purple", el.eval( "level2.color2", modelScope ) );
    }

    @Test
    public void test_conditional_methods()
    {
        MapBindings modelScope = new MapBindings()
                .withEntry( "a", new MapBindings()
                        .withEntry( "b", new MapBindings()
                                .withEntry( "$log", true )
                                .withEntry( "counter", 1 )
                                .withEntry( "$$countToTen", "$self.whileDo( () -> (counter < 10), () -> ($self.counter = counter + 1), 11 )" )
                                .withEntry( "$$raiseException", "$self.tryExcept( () -> c:raise( 'Hello' ), (e) -> ($self.message = e.message) ); message" )
                                .withEntry( "$$testIfThen", "$self.ifThen( () -> (counter == 10), () -> ($self.message = 'ifThenTrue') ); message" )
                                .withEntry( "$$testIfThenElse", "$self.ifThenElse( " +
                                        "() -> (counter == 10), " +
                                        "() -> ($self.message = 'ifThenElseTrue'), " +
                                        "() -> ($self.message = 'ifThenElseFalse') ); message" )
                        )
                );

        assertEquals( 1, el.eval( "a.b.counter", modelScope ) );
        assertEquals( 10L, el.eval( "a.b.countToTen(); a.b.counter", modelScope ) );
        assertEquals( "Hello", el.eval( "a.b.raiseException()", modelScope ) );
        assertEquals( "ifThenTrue", el.eval( "a.b.testIfThen()", modelScope ) );
        assertEquals( "ifThenElseTrue", el.eval( "a.b.testIfThenElse()", modelScope ) );
        el.eval( "a.b.counter = a.b.counter + 1", modelScope );
        assertEquals( "ifThenElseFalse", el.eval( "a.b.testIfThenElse()", modelScope ) );
    }

    @Test
    public void test_runnable_methods()
    {
        MapBindings modelScope = new MapBindings()
                .withEntry( "a", new MapBindings()
                        .withEntry( "$log", true )
                        .withEntry(
                                "$$testPutRunnables",
                                "$self.put( 'xxx', () -> c:println('hello from put runnable') );\n" +
                                        "$self.shout = ( message ) -> c:println( message );" )
                );

        // put the runnable
        el.eval( "a.testPutRunnables()", modelScope );
        el.eval( "a.xxx.run()", modelScope );
        el.eval( "a.xxx()", modelScope );
        el.eval( "a.shout( 'out loud' )", modelScope );
    }


    @Test
    public void test_listener()
    {
        el.addListeners( new SimpleEvaluationListener() );

        MapBindings modelScope = new MapBindings()
                .withEntry( "a", new MapBindings()
                        .withEntry( "b", new MapBindings()
                                .withEntry( "counter", 1 )
                                .withEntry( "$$countToTen", "$self.whileDo( () -> (counter < 10), () -> ($self.counter = counter + 1), 11 )" )
                                .withEntry( "$$raiseException", "$self.tryExcept( () -> c:raise( 'Hello' ), (e) -> ($self.message = e.message) ); message" )
                                .withEntry( "$$testIfThen", "$self.ifThen( () -> (counter == 10), () -> ($self.message = 'ifThenTrue') ); message" )
                                .withEntry( "$$testIfThenElse", "$self.ifThenElse( " +
                                        "() -> (counter == 10), " +
                                        "() -> ($self.message = 'ifThenElseTrue'), " +
                                        "() -> ($self.message = 'ifThenElseFalse') ); message" )
                        )
                );
        assertEquals( 1, el.eval( "a.b.counter", modelScope ) );
        assertEquals( 10L, el.eval( "a.b.countToTen(); a.b.counter", modelScope ) );
        assertEquals( "Hello", el.eval( "a.b.raiseException()", modelScope ) );
        assertEquals( "ifThenTrue", el.eval( "a.b.testIfThen()", modelScope ) );
        assertEquals( "ifThenElseTrue", el.eval( "a.b.testIfThenElse()", modelScope ) );
        el.eval( "a.b.counter = a.b.counter + 1", modelScope );
        assertEquals( "ifThenElseFalse", el.eval( "a.b.testIfThenElse()", modelScope ) );
    }

}
