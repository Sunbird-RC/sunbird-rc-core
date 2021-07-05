package io.opensaber.registry.middleware.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConditionResolverService {
    private static final Logger logger = LoggerFactory.getLogger(ConditionResolverService.class);

    /**
     * @param entityNode subject node where we will apply the extract out the values for given json path
     * @param matcher it accepts either ATTESTOR or REQUESTER
     * @param condition this is the condition the system has to resolve which will be used for evaluation
     * @param attributes contains pair[key, val] where key will be replaced with its value in the condition
     * */
    public String resolve(JsonNode entityNode, String matcher, String condition, List<String[]> attributes) {
        String entity = entityNode.toString();
        logger.info("Gonna resolve for the json {}", entity);
        condition = replaceMultipleEntries(condition, attributes);
        List<Integer> matchersIndices = findWordIndices(matcher, condition);
        List<String[]> matchersValuesPair = new ArrayList<>();
        for (int index : matchersIndices) {
            String[] expressions = generateExpressionAndJsonPathPair(index, condition);
            expressions[1] = replaceOriginalValueForGivenJsonPath(entity, expressions[1]);
            matchersValuesPair.add(expressions);
        }
        for(String[] pair: matchersValuesPair) {
            condition = replace(condition, pair);
        }
        return condition;
    }
    private String replaceOriginalValueForGivenJsonPath(String entity, String path) {
        Configuration alwaysReturnListConfig = Configuration.builder().options(Option.ALWAYS_RETURN_LIST).build();
        List<String> read = JsonPath.using(alwaysReturnListConfig).parse(entity).read(path);
        String s;
        if(read.size() == 1) {
            s = "'" + read.get(0) + "'";
        } else {
            s = read.toString();
        }
        return s.replace("[", "{").replace("]", "}");
    }

    private String[] generateExpressionAndJsonPathPair(int index, String condition) {
        int hashCount = 0;
        int indexAfterFirstHash = -1;
        String[] ans = new String[2];
        StringBuilder sb = new StringBuilder();
        for (int i = index; i < condition.length(); i++) {
            char val = condition.charAt(i);
            sb.append(val);
            if (val == '#') {
                hashCount++;
            }
            if (hashCount == 1 && indexAfterFirstHash == -1) {
                indexAfterFirstHash = i;
            } else if(hashCount == 2){
                ans[0] = sb.toString();
                ans[1] = condition.substring(indexAfterFirstHash + 1, i);
                break;
            }
        }
        return ans;
    }

    private String replaceMultipleEntries(String condition, List<String[]> attributes) {
        for (String[] entry : attributes) {
            condition = replace(condition, entry);
        }
        logger.info("Condition after replacing the entries {}", condition);
        return condition;
    }

    private String replace(String condition, String[] pair) {
        logger.info("Processing pairs condition {} -> pairs {}, {}", condition, pair[0], pair[1]);
        return condition.replace(pair[0], pair[1]);
    }

    private List<Integer> findWordIndices(String matcher, String condition) {
        int index = 0;
        List<Integer> indices = new ArrayList<>();
        while(index != -1) {
            index = condition.indexOf(matcher, index);
            if(index != -1) {
                indices.add(index);
                index++;
            }
        }
        return indices;
    }

    public boolean evaluate(String condition) {
        logger.info("Resolved conditions {}", condition);
        ExpressionParser expressionParser = new SpelExpressionParser();
        Expression expression = expressionParser.parseExpression(condition);
        return expression.getValue(Boolean.class);
    }
}
