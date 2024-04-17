package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.exception.EncryptionException;
import dev.sunbirdrc.registry.util.PrivateField;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "encryption.enabled", havingValue = "true")
public class DecryptionHelper extends PrivateField {

    public JsonNode getDecryptedJson(JsonNode rootNode) throws EncryptionException {
        String rootFieldName = rootNode.fieldNames().next();
        JsonNode updatedNode = process(rootNode.get(rootFieldName), rootFieldName, null);
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.set(rootFieldName, updatedNode);
        return objectNode;
    }

    protected Map<String, Object> performOperation(Map<String, Object> plainMap) throws EncryptionException {
        return encryptionService.decrypt(plainMap);
    }
}
