package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class ArrayHelperTest {

    @Test
    void testRemoveSquareBraces() {
        String expectedString = " hari,sri ram,giri";
        String actualString = ArrayHelper.removeSquareBraces("[ hari,sri ram,giri]");
        assertTrue(expectedString.equalsIgnoreCase(actualString));
    }

    @Test
    void testRemoveSquareBracesWithNull() {
        assertThrows(NullPointerException.class, () -> {
            ArrayHelper.removeSquareBraces(null);
        });
    }

    @Test
    void testFormatToString() {
        String expectedString = "[\" hari\",\"sri ram\",\"giri\"]";
        List<Object> inputLst = new ArrayList<>();
        inputLst.add(" hari");
        inputLst.add("sri ram");
        inputLst.add("giri");

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));
    }

    @Test
    void testFormatToStringSingle() {
        String expectedString = "[\"giri\"]";
        List<Object> inputLst = new ArrayList<>();
        inputLst.add("giri");

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));
    }

    @Test
    void testFormatToIntMultiple() {
        String expectedString = "[1,2,3]";
        List<Object> inputLst = new ArrayList<>();
        inputLst.add(1);
        inputLst.add(2);
        inputLst.add(3);

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));
    }

    @Test
    void isArrayTrue() {
        String stringArr = "[\"a\"]";
        assertTrue(ArrayHelper.isArray(stringArr));
    }

    @Test
    void isArrayFalse() {
        String stringArr = "a";
        assertFalse(ArrayHelper.isArray(stringArr));
    }

    @Test
    void testFormatToIntSingle() {
        String expectedString = "[1]";
        List<Object> inputLst = new ArrayList<>();
        inputLst.add(1);

        String actualString = ArrayHelper.formatToString(inputLst);
        assertTrue(expectedString.equalsIgnoreCase(actualString));
    }

    @Test
    void testFormatToStringWithNull() {
        assertThrows(NullPointerException.class, () -> {
            ArrayHelper.formatToString(null);
        });
    }

    @Test
    void testIsArraySingleValid() {
        assertTrue(ArrayHelper.isArray("[a]"));
    }

    @Test
    void constructIntegerArrayNode() {
        ArrayNode arrayNode = ArrayHelper.constructArrayNode("[1,2,3]");
        arrayNode.forEach(item -> {
            int val = item.intValue();
            assertTrue(val == 1 || val == 2 || val == 3);
        });
    }

    @Test
    void constructDoubleArrayNode() {
        ArrayNode arrayNode = ArrayHelper.constructArrayNode("[1.1,2.1,3.1]");
        arrayNode.forEach(item -> {
            Double val = item.doubleValue();
            assertTrue(val.compareTo(1.1) == 0 ||
                    val.compareTo(2.1) == 0 ||
                    val.compareTo(3.1) == 0);
        });
    }

    @Test
    void constructStringArrayNode() {
        ArrayNode arrayNode = ArrayHelper.constructArrayNode("[\"a\", \"b\", \"c\"]");
        arrayNode.forEach(item -> {
            String val = item.asText();
            assertTrue("a".equals(val) ||
                    "b".equals(val) ||
                    "c".equals(val));
        });
    }

    @Test
    void constructJsonStringArrayNode() {
        ArrayNode arrayNode = ArrayHelper.constructArrayNode("[{\"op\":\"add\",\"path\":\"/Teacher\"},{\"op\":\"update\",\"path\":\"/Teacher\"}]");

        arrayNode.forEach(item -> {
            assertTrue("add".equals(item.get("op").asText()) ||
                    "/Teacher".equals(item.get("path").asText()) ||
                    "update".equals(item.get("op").asText()) ||
                    "/Teacher".equals(item.get("path").asText())
            );
        });
    }

    @Test
    void unquoteStringWithQuotes() {
        String qStr = "\"a\"";
        String expected = "a";
        assertTrue(expected.compareToIgnoreCase(ArrayHelper.unquoteString(qStr)) == 0);
    }

    @Test
    void unquoteStringWithoutQuotes() {
        String qStr = "a";
        String actual = ArrayHelper.unquoteString(qStr);
        assertTrue(actual == "a");
    }
}