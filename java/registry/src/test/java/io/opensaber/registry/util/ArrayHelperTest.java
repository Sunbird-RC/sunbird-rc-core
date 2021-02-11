package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
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
        List<Object> inputLst = new ArrayList<>();
        inputLst.add(" hari");
        inputLst.add("sri ram");
        inputLst.add("giri");

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));
    }

    @Test
    public void testFormatToStringSingle(){
        String expectedString = "[\"giri\"]";
        List<Object> inputLst = new ArrayList<>();
        inputLst.add("giri");

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));
    }

    @Test
    public void testFormatToIntMultiple(){
        String expectedString = "[1,2,3]";
        List<Object> inputLst = new ArrayList<>();
        inputLst.add(1);
        inputLst.add(2);
        inputLst.add(3);

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));
    }

    @Test
    public void isArrayTrue() {
        String stringArr = "[\"a\"]";
        assertTrue(ArrayHelper.isArray(stringArr));
    }

    @Test
    public void isArrayFalse() {
        String stringArr = "a";
        assertFalse(ArrayHelper.isArray(stringArr));
    }

    @Test
    public void testFormatToIntSingle(){
        String expectedString = "[1]";
        List<Object> inputLst = new ArrayList<>();
        inputLst.add(1);

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));
    }

    @Test(expected = NullPointerException.class)
    public void testFormatToStringWithNull(){
       ArrayHelper.formatToString(null);
    }

    @Test
    public void testIsArraySingleValid() {
        assertTrue(ArrayHelper.isArray("[a]"));
    }

    @Test
    public void constructIntegerArrayNode() {
        ArrayNode arrayNode = ArrayHelper.constructArrayNode("[1,2,3]");
        arrayNode.forEach( item -> {
            int val = item.intValue();
            assertTrue(val == 1 || val == 2 || val == 3);
        });
    }

    @Test
    public void constructDoubleArrayNode() {
        ArrayNode arrayNode = ArrayHelper.constructArrayNode("[1.1,2.1,3.1]");
        arrayNode.forEach( item -> {
            Double val = item.doubleValue();
            assertTrue(val.compareTo(1.1) == 0 ||
                    val.compareTo(2.1) == 0 ||
                    val.compareTo(3.1) == 0);
        });
    }

    @Test
    public void constructStringArrayNode() {
        ArrayNode arrayNode = ArrayHelper.constructArrayNode("[\"a\", \"b\", \"c\"]");
        arrayNode.forEach( item -> {
            String val = item.asText();
            assertTrue("a".equals(val) ||
                    "b".equals(val) ||
                    "c".equals(val));
        });
    }

    @Test
    public void constructJsonStringArrayNode() {
        ArrayNode arrayNode = ArrayHelper.constructArrayNode("[{\"op\":\"add\",\"path\":\"/Teacher\"},{\"op\":\"update\",\"path\":\"/Teacher\"}]");
       
        arrayNode.forEach( item -> { 
            assertTrue("add".equals(item.get("op").asText()) ||
            		"/Teacher".equals(item.get("path").asText()) ||
                    "update".equals(item.get("op").asText()) ||
                    "/Teacher".equals(item.get("path").asText())
                    );
        });
    }

    @Test
    public void unquoteStringWithQuotes() {
        String qStr = "\"a\"";
        String expected = "a";
        assertTrue(expected.compareToIgnoreCase(ArrayHelper.unquoteString(qStr)) == 0);
    }

    @Test
    public void unquoteStringWithoutQuotes() {
        String qStr = "a";
        String actual = ArrayHelper.unquoteString(qStr);
        assertTrue(actual == "a");
    }
}
