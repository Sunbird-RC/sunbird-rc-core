package io.opensaber.registry.util;

import org.apache.commons.lang3.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class creates util methods for String modification and replacing
 */
public class ArrayHelper {

    private static final String SQUARE_BRACE_REGEX = "[\\[\\]]";
    private static final String EMPTY_STR = "";

    /**
     * This method checks the input String in array format and removes the characters "[", "]"
     *
     * @param input as any String content
     * @return string replaced with square braces with empty character
     */
    public static String removeSquareBraces(String input) {
        Pattern pattern = Pattern.compile(SQUARE_BRACE_REGEX);
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll(EMPTY_STR);
    }

    /**This method creates String from the input list, no white space is allowed as prefix when each element is appeneded
     * @param inputList - which contains list of Strings
     * @return - String, in array format
     */
    public static String formatToString(List<String> inputList){
        List<String> quotedStr = new ArrayList<>();
        inputList.forEach(input -> {
            input = "\"" + input + "\"";
            quotedStr.add(input);
        });
        StringBuilder sb = new StringBuilder(StringUtils.join(quotedStr, ','));
        return sb.insert(0,'[').append(']').toString();
    }
}
