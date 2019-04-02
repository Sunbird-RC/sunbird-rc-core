package io.opensaber.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * OpenSABER value type helper
 * This class helps write and read the values in their original types,
 * which otherwise would be treated as plain strings.
 */
public class ValueType {
    /**
     * Writes into the database in the original value type that was passed
     * @param entryVal
     * @return
     */
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


    /**
     * Sets the contentNode to the corresponding value.
     * This is needed to appropriately identify the value types - long, double, string
     * @param contentNode - the node where the given fieldname value must be set
     * @param fieldName - the fieldname
     * @param readVal - the value type
     */
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
        } else if (readVal instanceof JsonNode) {
            contentNode.set(fieldName, (JsonNode) readVal);
        }
    }
}

