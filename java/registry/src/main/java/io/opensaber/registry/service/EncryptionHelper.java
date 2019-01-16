package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.util.Definition;
import io.opensaber.registry.util.DefinitionsManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncryptionHelper {
    @Autowired
    private EncryptionService encryptionService;
    @Autowired
    private DefinitionsManager definitionsManager;

    public JsonNode getEncryptedJson(JsonNode rootNode) throws EncryptionException {
        JsonNode encryptedRoot = rootNode;
        String rootFieldName = rootNode.fieldNames().next();
        Definition definition = definitionsManager.getDefinition(rootFieldName);
        List<String> privatePropertyLst = definition.getOsSchemaConfiguration().getPrivateFields();
        if (rootNode.isObject()) {
            Map<String, Object> plainMap = getToBeEncryptedMap(rootNode, privatePropertyLst);
            if(null != plainMap){
                Map<String, Object> encodedMap = encryptionService.encrypt(plainMap);
                encryptedRoot  = replaceWithEncryptedValues(rootNode, privatePropertyLst, encodedMap);
            }
        }
        return encryptedRoot;
    }

    /**
     * Identifies the keys in the rootNode that needs to be encrypted.
     * @param rootNode
     * @param privatePropertyLst
     * @return the keys and values that need to be encrypted
     */
    private Map<String, Object> getToBeEncryptedMap(JsonNode rootNode, List<String> privatePropertyLst) {
        Map<String, Object> plainKeyValues = new HashMap<>();
        rootNode.fields().forEachRemaining(entry -> {
            JsonNode entryValue = entry.getValue();
            if (entryValue.isValueNode()) {
                if (privatePropertyLst.contains(entry.getKey()))
                    plainKeyValues.put(entry.getKey(), entryValue.asText());
            } else if (entryValue.isObject()) {
                plainKeyValues.putAll(getToBeEncryptedMap(entryValue, privatePropertyLst));
            }
        });
        return plainKeyValues;
    }

    /**
     * Given the root node, based on the privatePropertyList, fetch the encrypted value
     * and replace the root node value (original unencrypted).
     *
     * @param rootNode
     * @param privatePropertyLst
     * @param encodedMap Contains the values encrypted
     */
    private JsonNode replaceWithEncryptedValues(JsonNode rootNode, List<String> privatePropertyLst, Map<String, Object> encodedMap) {
        JsonNode encryptedRootNode = rootNode;

        encryptedRootNode.fields().forEachRemaining(entry -> {
            JsonNode entryValue = entry.getValue();

            if (entryValue.isValueNode() && privatePropertyLst.contains(entry.getKey())) {
                // We encrypt only string nodes.
                String encryptedVal = encodedMap.get(entry.getKey()).toString();
                JsonNode encryptedValNode = JsonNodeFactory.instance.textNode(encryptedVal);
                entry.setValue(encryptedValNode);
            } else if (entryValue.isObject()) {
                replaceWithEncryptedValues(entryValue, privatePropertyLst, encodedMap);
            }
        });
        return encryptedRootNode;
    }
}
