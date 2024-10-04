package dev.sunbirdrc.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class FieldTest {

    private Field field = new Field();

    @Test
    public void testGetFunctioName(){
        field.setFunction("#/functionDefinitions/concat($lastName, $firstName)");
        String name = field.getFunctioName();
        assertEquals("concat", name);
    }

    @Test
    public void testGetFunctioNameEmpty(){
        field.setFunction("#/functionDefinitions/($lastName, $firstName)");
        assertThrows(IllegalArgumentException.class, () -> {
            field.getFunctioName();
        });
    }
}