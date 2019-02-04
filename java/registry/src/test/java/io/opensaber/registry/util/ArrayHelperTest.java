package io.opensaber.registry.util;

import java.util.*;
import org.junit.*;
import org.junit.runner.*;
import org.springframework.test.context.junit4.*;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class ArrayHelperTest {

    @Test
    public void testRemoveSquareBraces() {
        String expectedString = " hari,sri ram,giri";
        String actualString = ArrayHelper.removeSquareBraces("[ hari,sri ram,giri]");
        assertTrue(expectedString.equalsIgnoreCase(actualString));
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveSquareBracesWithNull() {
        ArrayHelper.removeSquareBraces(null);
    }

    @Test
    public void testFormatToString(){
        String expectedString = "[ hari,sri ram,giri]";
        List<String> inputLst = new ArrayList<>();
        inputLst.add(" hari");
        inputLst.add("sri ram");
        inputLst.add("giri");

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));

    }

    @Test(expected = NullPointerException.class)
    public void testFormatToStringWithNull(){
       ArrayHelper.formatToString(null);
    }
}
