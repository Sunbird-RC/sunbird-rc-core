package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ValueType {
    public static Object getValue(JsonNode entryVal) {
        Object result = null;
        if (!entryVal.isValueNode()) {
            // If it is not a value node, simply return input
            result = entryVal;
            return result;
        }

        if (entryVal.isBoolean()) {
            result = entryVal.asBoolean();
        } else if (entryVal.isTextual()) {
            result = entryVal.textValue();
        } else if (entryVal.isIntegralNumber() ||
                    entryVal.isLong() ||
                    entryVal.isBigInteger()) {
            // Any number
            result = entryVal.asLong();
        } else if ((!entryVal.isIntegralNumber() && entryVal.isNumber()) ||
                    entryVal.isFloat() ||
                    entryVal.isBigDecimal()) {
            // Decimal number
            result = entryVal.asDouble();
        }
        return result;
    }

    public static void setValue(ObjectNode contentNode, String fieldName, Object readVal) {
        if (readVal instanceof Boolean) {
            contentNode.put(fieldName, (Boolean) readVal);
        } else if (readVal instanceof Long) {
            contentNode.put(fieldName, (Long) readVal);
        } else if (readVal instanceof Integer) {
            contentNode.put(fieldName, (Integer) readVal);
        } else if (readVal instanceof Double) {
            contentNode.put(fieldName, (Double) readVal);
        } else if (readVal instanceof String) {
            contentNode.put(fieldName, (String) readVal);
        }
    }
}
