package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates util methods for String modification and replacing
 */
public class ArrayHelper {

	private static Logger logger = LoggerFactory.getLogger(ArrayHelper.class);
	
    private static final String ITEM_SEPARATOR = ",";
    private static final String SQUARE_BRACE_REGEX = "[\\[\\]]";
    private static final String SQUARE_BRACE_ENCLOSED_REGEX = "(\\[)(.*)(\\])";
    private static final String EMPTY_STR = "";
    private static final Pattern pattern = Pattern.compile(SQUARE_BRACE_REGEX);

    /**
     * This method checks the input String in array format and removes the characters "[", "]"
     *
     * @param input as any String content
     * @return string replaced with square braces with empty character
     */
    public static String removeSquareBraces(String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll(EMPTY_STR);
    }

    /**This method creates String from the input list, no white space is allowed as prefix when each element is appeneded
     * @param inputList - which contains list of Strings
     * @return - String, in array format
     */
    public static String formatToString(List<Object> inputList) {
        List<Object> quotedStr = new ArrayList<>();
        if (inputList.size() > 0) {
            boolean isString = !isNotAString(inputList.get(0).toString());
            inputList.forEach(input -> {
                if (isString) {
                    input = "\"" + input + "\"";
                }
                quotedStr.add(input);
            });
        }
        StringBuilder sb = new StringBuilder(StringUtils.join(quotedStr, ITEM_SEPARATOR));
        return sb.insert(0,'[').append(']').toString();
    }

    /**
     * Removes the quotes from the beginning and end of the quoted str
     * @param quotedStr
     * @return
     */
    public static String unquoteString(String quotedStr) {
    	if(quotedStr.startsWith("\"") && quotedStr.endsWith("\"")){
    		return StringUtils.substringBetween(quotedStr, "\"", "\"");
    	}
        return quotedStr;
    }

    /**
     * Flags whether a passed in string is an array representation.
     * An array string representation is like [1,2,3] or ["a"]
     * @param valueStr
     * @return
     */
    public static boolean isArray(String valueStr) {
        return Pattern.matches(SQUARE_BRACE_ENCLOSED_REGEX, valueStr);
    }

    /**
     * Checks whether an array representation contains string values or non-string values
     * like, integers, double, long, float
     * @param value
     * @return
     */
    private static boolean isNotAString(String value) {
        return Pattern.matches("[+-]?([0-9]*[.])?[0-9]+", value);
    }

    /**
     *
     * @param valItems example, "[1,2,3]" or "["social", "english"]" or "[{"op":"add","path":"/Teacher"}]"
     * @return
     */
    public static ArrayNode constructArrayNode(String valItems) {
    	ArrayNode arrNode = JsonNodeFactory.instance.arrayNode();
    	ObjectMapper mapper = new ObjectMapper();
    	
    	try {
        	List<Object> itemList =  mapper.readValue(valItems, 
            		TypeFactory.defaultInstance().constructCollectionType(List.class, Object.class));     
        	 arrNode = mapper.valueToTree(itemList);
        } catch (Exception e) {
            logger.error("Error in converting array elements to JsonNode" + e);
        }
        return arrNode;
    }
}