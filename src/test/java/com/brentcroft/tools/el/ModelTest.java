package com.brentcroft.tools.el;

import com.brentcroft.tools.model.ModelItem;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class ModelTest
{
    private final ModelItem item = new ModelItem();

    @Before
    public void setCurrentDirectory() {
        item.setCurrentDirectory( Paths.get( "src/test/resources/models" ) );
        item.getStaticModel().clear();
    }

    @Test
    public void loadsModel() {
        item.appendFromJson( "{ '$xml': 'model-01.xml' }" );
        assertEquals( "model-01", item.eval("$title") );
    }

    @Test
    public void testModelLambdas() {
        item.appendFromJson( "{ '$xml': 'model-01.xml' }" );
        item.eval("$self.testLambdaArgs()" );
    }

    @Test
    public void testStaticLambdas() {
        item.appendFromJson( "{ '$xml': 'model-02.xml' }" );
        item.eval("$self.testStaticLambdas()" );
    }

    @Test
    public void testHierarchy() {
        item.appendFromJson( "{ '$xml': 'model-03-hierarchy.xml' }" );
        item.eval("$self.testHierarchy01()" );
        item.eval("$self.testHierarchy02()" );
        item.eval("$self.testHierarchy03()" );
    }

    @Test
    public void testHierarchyConditionals() {
        item.appendFromJson( "{ '$xml': 'model-03-hierarchy.xml' }" );
        item.eval("$self.testHierarchyConditionals()" );
    }
}
