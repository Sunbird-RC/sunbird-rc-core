package org.egov.id.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdGenUtils {

    public static List<String> getMatchList(String idFormat) {
        List<String> matchList = new ArrayList<String>();

        Pattern regExpPattern = Pattern.compile("\\[(.*?)\\]");
        Matcher regExpMatcher = regExpPattern.matcher(idFormat);

        while (regExpMatcher.find()) {// Finds Matching Pattern in String
            matchList.add(regExpMatcher.group(1));// Fetching Group from String
        }
        return matchList;
    }
}
