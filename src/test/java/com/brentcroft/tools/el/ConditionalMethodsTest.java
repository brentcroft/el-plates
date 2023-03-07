package com.brentcroft.tools.el;

import com.brentcroft.tools.jstl.MapBindings;
import com.brentcroft.tools.model.ModelItem;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ConditionalMethodsTest
{
    private final ModelItem item = new ModelItem();
    private final ELTemplateManager el = new ELTemplateManager();

    @Before
    public void setCurrentDirectory()
    {
        item.setCurrentDirectory( Paths.get( "src/test/resources/models" ) );
        item.getStaticModel().clear();
    }

    @Test
    public void test_thread_local_resolver()
    {
        Stack< Map< String, Object > > stack = new Stack<>();
        stack.push( new HashMap<>() );

        el.addPrimaryResolvers( new ThreadLocalRootResolver( ThreadLocal.withInitial( () -> stack ) ) );

        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "color1", "blue" )
                        .withEntry( "color2", "orange" )
                        .withEntry( "color3", "yellow" )
                        .withEntry( "color4", "obtuse" )
                );

        stack.push( scope );

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
    public void test_return()
    {
        Stack< Map< String, Object > > stack = new Stack<>();
        stack.push( new HashMap<>() );

        ThreadLocal< Stack< Map< String, Object > > > scopeStack = ThreadLocal.withInitial( () -> stack );
        MapStepsELResolver tlsELResolver = new MapStepsELResolver( el, el );
        el.addPrimaryResolvers( tlsELResolver );
        el.addSecondaryResolvers( new ConditionalMethodsELResolver( scopeStack ) );

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
    public void test_assignment()
    {
        Stack< Map< String, Object > > stack = new Stack<>();
        stack.push( new HashMap<>() );

        ThreadLocal< Stack< Map< String, Object > > > scopeStack = ThreadLocal.withInitial( () -> stack );
        el.addPrimaryResolvers(
                new MapMethodELResolver()
        );
        el.addSecondaryResolvers(
                new CompiledStepsResolver( scopeStack ),
                new ConditionalMethodsELResolver( scopeStack ),
                new MapStepsELResolver( el, el ) );

        MapBindings bindings = new MapBindings().withEntry( "colors", new MapBindings() );

        el.eval( "colors.time = 10; colors.whileDo( ()-> colors.time < 20, () -> (colors.time = colors.time + 1 ), 12 )", bindings );
        assertEquals( 20L, el.eval( "colors.time", bindings ) );

        el.eval( "colors.whileDo( () -> true, () -> c:delay(500), 1, ( s ) -> ( timeWaiting = s ) )", bindings );

        Double timeWaiting = ( Double ) el.eval( "colors.timeWaiting", bindings );

        assertTrue(
                format( "Time waiting is too large: %s", timeWaiting ),
                Math.abs( 0.5 - timeWaiting ) < 0.1
        );
    }


    @Test
    public void test_thread_local_resolver_steps()
    {
        MapStepsELResolver tlsELResolver = new MapStepsELResolver( el, el );
        el.addPrimaryResolvers( tlsELResolver );

        MapBindings scope = new MapBindings()
                .withEntry( "colors", new MapBindings()
                        .withEntry( "x", 1 )
                        .withEntry( "y", 2 )
                        .withEntry( "$$helloWorld", "c:println('hello world '.concat($self.plus()));\n# comments \n $self.minus({'x':100,'y':37})" )
                        .withEntry( "$$plus", "x + y" )
                        .withEntry( "$$minus", "x - y" ) );

        assertEquals( 63L, el.eval( "colors.helloWorld()", scope ) );
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

        ParentedMapELResolver staticResolver = new ParentedMapELResolver( scope );

        el.addSecondaryResolvers( staticResolver );

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

        MapBindings stackScope = new MapBindings()
                .withEntry( "color1", "blue" );

        Stack< Map< String, Object > > stack = new Stack<>();
        stack.push( stackScope );

        ThreadLocal< Stack< Map< String, Object > > > scopeStack = ThreadLocal.withInitial( () -> stack );
        el.addPrimaryResolvers( new ThreadLocalRootResolver( scopeStack ) );
        el.addSecondaryResolvers( new SimpleMapELResolver( staticScope ) );

        MapBindings modelScope = new MapBindings()
                .withEntry( "color1", "yellow" )
                .withEntry( "color2", "orange" )
                .withEntry( "color9", "vermillion" )
                .withEntry( "level2", new MapBindings()
                        .withEntry( "color1", "puce" )
                        .withEntry( "color2", "purple" )
                );

        assertEquals( "blue", el.eval( "color1", modelScope ) );
        assertEquals( "orange", el.eval( "color2", modelScope ) );
        assertEquals( "red", el.eval( "color3", modelScope ) );

        assertEquals( "puce", el.eval( "level2.color1", modelScope ) );
        assertEquals( "purple", el.eval( "level2.color2", modelScope ) );
        assertEquals( "vermillion", el.eval( "level2.color9", modelScope ) );
    }

    @Test
    public void test_conditional_methods()
    {
        Stack< Map< String, Object > > stack = new Stack<>();
        stack.push( new HashMap<>() );

        ThreadLocal< Stack< Map< String, Object > > > scopeStack = ThreadLocal.withInitial( () -> stack );
        MapStepsELResolver tlsELResolver = new MapStepsELResolver( el, el );
        el.addPrimaryResolvers( tlsELResolver );
        el.addSecondaryResolvers( new ConditionalMethodsELResolver( scopeStack ) );

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

    @Test
    public void test_runnable_methods()
    {
        Stack< Map< String, Object > > stack = new Stack<>();
        stack.push( new HashMap<>() );

        ThreadLocal< Stack< Map< String, Object > > > scopeStack = ThreadLocal.withInitial( () -> stack );
        el.addPrimaryResolvers(
                new MapMethodELResolver()
        );
        el.addSecondaryResolvers(
                new CompiledStepsResolver( scopeStack ),
                new ConditionalMethodsELResolver( scopeStack ),
                new MapStepsELResolver( el, el ) );

        MapBindings modelScope = new MapBindings()
                .withEntry( "a", new MapBindings()
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
        Stack< Map< String, Object > > stack = new Stack<>();
        stack.push( new HashMap<>() );

        ThreadLocal< Stack< Map< String, Object > > > scopeStack = ThreadLocal.withInitial( () -> stack );
        MapStepsELResolver tlsELResolver = new MapStepsELResolver( el, el );
        el.addPrimaryResolvers( tlsELResolver );

        el.addSecondaryResolvers( new ConditionalMethodsELResolver( scopeStack ) );

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
