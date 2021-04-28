package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.util.PrivateField;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EncryptionHelper extends PrivateField {
    protected Map<String, Object> performOperation(Map<String, Object> plainMap) throws EncryptionException {
        return encryptionService.encrypt(plainMap);
    }

    public JsonNode getEncryptedJson(JsonNode rootNode) throws EncryptionException {
        String rootFieldName = rootNode.fieldNames().next();
        process(rootNode.get(rootFieldName), rootFieldName, null);

        return rootNode;
    }
}
