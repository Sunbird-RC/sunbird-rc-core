package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.registry.service.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrivateField {

    @Autowired
    public EncryptionService encryptionService;
    @Autowired
    public DefinitionsManager definitionsManager;

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
            } else if (entryValue.isObject()) {
                plainKeyValues.putAll(getPrivateFields(entryValue, privatePropertyLst));
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
            } else if (entryValue.isObject()) {
                replacePrivateFields(entryValue, privatePropertyLst, privateFieldMap);
            }
        });
        return rootNode;
    }
}
