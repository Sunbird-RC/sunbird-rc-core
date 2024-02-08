package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import dev.sunbirdrc.registry.exception.EncryptionException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.service.EncryptionService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if(privatePropertyLst != null) {
            DocumentContext documentContext = JsonPath.parse(rootNode.toString());
            privatePropertyLst.forEach(path -> {
                Object read = documentContext.read(path);
                plainKeyValues.put(path, read);
            });
        }
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
    public JsonNode replacePrivateFields(JsonNode rootNode, List<String> privatePropertyLst, Map<String, Object> privateFieldMap) throws IOException {
        if (privatePropertyLst != null) {
            DocumentContext documentContext = JsonPath.parse(rootNode.toString());
            privateFieldMap.forEach(documentContext::set);
            return JSONUtil.convertStringJsonNode(documentContext.jsonString());
        }
        return rootNode;
    }

    protected Map<String, Object> performOperation(Map<String, Object> plainMap) throws EncryptionException {
        return null;
    }

    protected JsonNode processPrivateFields(JsonNode element, String rootDefinitionName, String childFieldName) throws EncryptionException {
        JsonNode tempElement = element;
        Definition definition = definitionsManager.getDefinition(rootDefinitionName);;
        if (null != childFieldName && definition != null) {
            String defnName = definition.getDefinitionNameForField(childFieldName);
            Definition childDefinition = definitionsManager.getDefinition(defnName);
            if (null == childDefinition) {
                logger.info("Cannot get child name definition {}", childFieldName);
                return element;
            }
            definition = childDefinition;
        } else if (definition == null) {
            return element;
        }

        List<String> privatePropertyLst = definition.getOsSchemaConfiguration().getPrivateFields()
                .stream().map(d -> {
                    if (!d.startsWith("$."))
                        return String.format("$.%s", d.replaceAll("/", "."));
                    return d;
                }).collect(Collectors.toList());
        Map<String, Object> plainMap = getPrivateFields(element, privatePropertyLst);
        if (null != plainMap && !plainMap.isEmpty()) {
            Map<String, Object> encodedMap = performOperation(plainMap);
            try {
                tempElement = replacePrivateFields(element, privatePropertyLst, encodedMap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return tempElement;
    }

    private ArrayNode processArray(ArrayNode arrayNode, String rootFieldName, String fieldName) throws EncryptionException {
        ArrayNode updatedArrayNode = JsonNodeFactory.instance.arrayNode();
        for (JsonNode jsonNode : arrayNode) {
            JsonNode updatedNode = jsonNode;
            if (jsonNode.isObject()) {
                updatedNode = process(jsonNode, rootFieldName, fieldName);
            }
            updatedArrayNode.add(updatedNode);
        }
        return updatedArrayNode;
    }

    protected JsonNode process(JsonNode jsonNode, String rootFieldName, String fieldName) throws EncryptionException {
        jsonNode = processPrivateFields(jsonNode, rootFieldName, fieldName);

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
                    entry.setValue(process(entryValue, tempFieldName, entry.getKey()));
                } else if (isNotSignatures && entryValue.isArray()) {
                    entry.setValue(processArray((ArrayNode) entryValue, tempFieldName, entry.getKey()));
                }
            } catch (EncryptionException e) {
                logger.error("Exception occurred in PrivateField: {}", ExceptionUtils.getStackTrace(e));
            }
        }
        return jsonNode;
    }
}
