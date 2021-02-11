package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.registry.exception.SignatureException;
import io.opensaber.registry.middleware.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class SignatureHelper {
    @Autowired
    private SignatureService signatureService;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${signature.keysURL}")
    private String signatureKeyURl;
    @Value("${registry.context.base}")
    private String registryContextBase;
    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;

    /**
     * Signs the entity and returns the entity with signed json appending
     *
     * @param rootNode
     * @return The new signature created
     * @throws SignatureException.UnreachableException
     * @throws SignatureException.CreationException
     */
    public JsonNode signJson(JsonNode rootNode) throws SignatureException.UnreachableException, SignatureException.CreationException {
        String entityType = rootNode.fieldNames().next();
        Map signRes = generateSignature(rootNode);

        JsonNode newSignatureNode = convertMapToNode(entityType, signRes);
        addSignature(rootNode, entityType, newSignatureNode);

        return newSignatureNode;
    }

    /**
     * Invokes the SignatureService to generate the signature for this entity
     *
     * @param rootNode
     * @return
     * @throws SignatureException.UnreachableException
     * @throws SignatureException.CreationException
     */

    private Map generateSignature(JsonNode rootNode) throws SignatureException.UnreachableException, SignatureException.CreationException {
        Map signReq = new HashMap<String, Object>();
        signReq.put("entity", rootNode);
        Map<String, Object> signMap = (Map<String, Object>) signatureService.sign(signReq);

        return signMap;
    }

    public String getEntitySignaturePrefix() {
        return registryContextBase;
    }

    /**
     * Converts map to signature node
     *
     * @param entityType Entity name for which the signature node needs to be generated
     * @param signMap    The response got from signature service
     * @return
     */
    private JsonNode convertMapToNode(String entityType, Map<String, Object> signMap) {
        Map<String, Object> entitySignMap = new HashMap<>();
        entitySignMap.put(Constants.SIGN_SIGNATURE_VALUE, signMap.get(Constants.SIGN_SIGNATURE_VALUE));
        entitySignMap.put(Constants.SIGN_CREATOR, signatureKeyURl + signMap.get("keyId"));
        entitySignMap.put(Constants.SIGNATURE_FOR, registryContextBase + entityType);
        entitySignMap.put(Constants.TYPE_STR_JSON_LD, "RSASignature2018");
        entitySignMap.put(Constants.SIGN_CREATED_TIMESTAMP, Instant.now().toString());
        entitySignMap.put(Constants.SIGN_NONCE, "");
        JsonNode entitySignNode = objectMapper.convertValue(entitySignMap, JsonNode.class);
        return entitySignNode;
    }

    /**
     * Merges sign data to entity json
     *
     * @param entityNode
     * @param toAdd
     */
    private void addSignature(JsonNode entityNode, String entityType, JsonNode toAdd) {
        ArrayNode existingSignatures = (ArrayNode) entityNode.get(entityType).get(Constants.SIGNATURES_STR);
        boolean signatureExists = (existingSignatures != null);

        if (signatureExists) {
            existingSignatures.add(toAdd);
        } else {
            existingSignatures = JsonNodeFactory.instance.arrayNode();
            existingSignatures.add(toAdd);
            ((ObjectNode) entityNode.get(entityType)).set(Constants.SIGNATURES_STR, existingSignatures);
        }
    }

    /**
     * Gets the signature for the supplied itemName
     *
     * @param itemName
     * @param signaturesArr
     * @return
     */
    public JsonNode getItemSignature(String itemName, JsonNode signaturesArr) {
        JsonNode entitySignature = null;
        if (null != signaturesArr && signaturesArr.isArray()) {
            for (JsonNode signatureItem : signaturesArr) {
                if (signatureItem.get(Constants.SIGNATURE_FOR).textValue().contains(itemName)) {
                    entitySignature = signatureItem;
                }
            }
        }
        return entitySignature;
    }

    /**
     * Removes the entity signature from the node
     *
     * @param entityNodeType
     * @param node
     */
    public String removeEntitySignature(String entityNodeType, ObjectNode node) {
        String entitySignatureUUID = "";
        ArrayNode signatureArr = (ArrayNode) node.get(entityNodeType).get(Constants.SIGNATURES_STR);
        if (null != signatureArr && !signatureArr.isNull()) {
            int entitySignatureIdx = -1;
            for (int itr = 0; itr < signatureArr.size(); itr++) {
                JsonNode signature = signatureArr.get(itr);
                if (signature.get(Constants.SIGNATURE_FOR).toString().contains(entityNodeType)) {
                    entitySignatureIdx = itr;
                    entitySignatureUUID = signature.get(uuidPropertyName).textValue();
                    break;
                }
            }
            if (entitySignatureIdx != -1) {
                signatureArr.remove(entitySignatureIdx);
            }
        }
        return entitySignatureUUID;
    }
}