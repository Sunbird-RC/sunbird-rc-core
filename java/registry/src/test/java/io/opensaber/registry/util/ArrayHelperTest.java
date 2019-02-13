package io.opensaber.registry.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

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
        String expectedString = "[\" hari\",\"sri ram\",\"giri\"]";
        List<String> inputLst = new ArrayList<>();
        inputLst.add(" hari");
        inputLst.add("sri ram");
        inputLst.add("giri");

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));

    }

    @Test
    public void testFormatToStringSingle(){
        String expectedString = "[\"giri\"]";
        List<String> inputLst = new ArrayList<>();
        inputLst.add("giri");

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));

    }

    @Test(expected = NullPointerException.class)
    public void testFormatToStringWithNull(){
       ArrayHelper.formatToString(null);
    }
}
