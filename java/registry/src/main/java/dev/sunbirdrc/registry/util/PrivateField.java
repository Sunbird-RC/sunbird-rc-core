package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.sunbirdrc.registry.exception.EncryptionException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PrivateField {
    @Autowired
    public EncryptionService encryptionService;
    @Autowired
    public IDefinitionsManager definitionsManager;
    private Logger logger = LoggerFactory.getLogger(PrivateField.class);

    /**
     * Identifies the keys in the rootNode that needs to be encrypted/decrypted
     * @param rootNode
     * @param privatePropertyLst
     * @return the keys and values that need to be encrypted/decrypted based on base call
     */
    public Map<String, Object> getPrivateFields(JsonNode rootNode, List<String> privatePropertyLst) {
        Map<String, Object> plainKeyValues = new HashMap<>();
        rootNode.fields().forEachRemaining(entry -> {
            JsonNode entryValue = entry.getValue();
            if (entryValue.isValueNode()) {
                if (privatePropertyLst.contains(entry.getKey()))
                    plainKeyValues.put(entry.getKey(), entryValue.asText());
            }
        });
        return plainKeyValues;
    }

    /**
     * Given the root node, based on the privatePropertyList, fetch the encrypted/decrypted value.
     * and replace the root node value (original unencrypted/decrypted)
     *
     * @param rootNode
     * @param privatePropertyLst
     * @param privateFieldMap Contains the values encrypted/decrypted based on base call
     */
    public JsonNode replacePrivateFields(JsonNode rootNode, List<String> privatePropertyLst, Map<String, Object> privateFieldMap) {

        rootNode.fields().forEachRemaining(entry -> {
            JsonNode entryValue = entry.getValue();

            if (entryValue.isValueNode() && privatePropertyLst.contains(entry.getKey())) {
                String privateFieldValue = privateFieldMap.get(entry.getKey()).toString();
                JsonNode encryptedValNode = JsonNodeFactory.instance.textNode(privateFieldValue);
                entry.setValue(encryptedValNode);
            }
        });
        return rootNode;
    }

    protected Map<String, Object> performOperation(Map<String, Object> plainMap) throws EncryptionException {
        return null;
    }

    protected JsonNode processPrivateFields(JsonNode element, String rootDefinitionName, String childFieldName) throws EncryptionException {
        JsonNode tempElement = element;
        Definition definition = definitionsManager.getDefinition(rootDefinitionName);;
        if (null != childFieldName) {
            String defnName = definition.getDefinitionNameForField(childFieldName);
            Definition childDefinition = definitionsManager.getDefinition(defnName);
            if (null == childDefinition) {
                logger.error("Cannot get child name definition {}", childFieldName);
                return element;
            }
            definition = childDefinition;
        }

        List<String> privatePropertyLst = definition.getOsSchemaConfiguration().getPrivateFields();
        Map<String, Object> plainMap = getPrivateFields(element, privatePropertyLst);
        if (null != plainMap && !plainMap.isEmpty()) {
            Map<String, Object> encodedMap = performOperation(plainMap);
            tempElement = replacePrivateFields(element, privatePropertyLst, encodedMap);
        }
        return tempElement;
    }

    private void processArray(ArrayNode arrayNode, String rootFieldName, String fieldName) throws EncryptionException {
        for (JsonNode jsonNode : arrayNode) {
            if (jsonNode.isObject()) {
                process(jsonNode, rootFieldName, fieldName);
            }
        }
    }

    protected JsonNode process(JsonNode jsonNode, String rootFieldName, String fieldName) throws EncryptionException {
        processPrivateFields(jsonNode, rootFieldName, fieldName);

        String tempFieldName = fieldName;
        if (null == tempFieldName) {
            tempFieldName = rootFieldName;
        }

        JsonNode toProcess = jsonNode;
        JsonNode childNode = jsonNode.get(tempFieldName);
        if (null != childNode) {
            toProcess = childNode;
        }

        Iterator<Map.Entry<String, JsonNode>> fieldsItr = toProcess.fields();
        while (fieldsItr.hasNext()) {
            try {
                Map.Entry<String, JsonNode> entry = fieldsItr.next();
                JsonNode entryValue = entry.getValue();
                logger.debug("Processing {}.{} -> {}", tempFieldName, entry.getKey(), entry.getValue());
                boolean isNotSignatures = !Constants.SIGNATURES_STR.equals(entry.getKey());

                if (isNotSignatures && entryValue.isObject()) {
                    // Recursive calls
                    process(entryValue, tempFieldName, entry.getKey());
                } else if (isNotSignatures && entryValue.isArray()) {
                    processArray((ArrayNode) entryValue, tempFieldName, entry.getKey());
                }
            } catch (EncryptionException e) {
                e.printStackTrace();
            }
        }
        return jsonNode;
    }
}
