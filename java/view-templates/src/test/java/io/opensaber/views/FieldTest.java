package io.opensaber.views;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FieldTest {
    
    private Field field = new Field();
    
    @Test
    public void testGetFunctioName(){
        field.setFunction("#/functionDefinitions/concat($lastName, $firstName)");
        String name = field.getFunctioName();
        assertEquals("concat", name);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetFunctioNameEmpty(){
        field.setFunction("#/functionDefinitions/($lastName, $firstName)");
        field.getFunctioName();
    }

}
