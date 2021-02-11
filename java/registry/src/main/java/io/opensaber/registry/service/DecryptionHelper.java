package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.util.Definition;
import io.opensaber.registry.util.PrivateField;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DecryptionHelper extends PrivateField {

    public JsonNode getDecryptedJson(JsonNode rootNode) throws EncryptionException {
        JsonNode decryptedRoot = rootNode;
        String rootFieldName = rootNode.fieldNames().next();
        Definition definition = definitionsManager.getDefinition(rootFieldName);
        List<String> privatePropertyLst = definition.getOsSchemaConfiguration().getPrivateFields();
        if (rootNode.isObject()) {
            Map<String, Object> plainMap = getPrivateFields(rootNode, privatePropertyLst);
            if(null != plainMap){
                Map<String, Object> encodedMap = encryptionService.decrypt(plainMap);
                decryptedRoot  = replacePrivateFields(rootNode, privatePropertyLst, encodedMap);
            }
        }
        return decryptedRoot;
    }

}
