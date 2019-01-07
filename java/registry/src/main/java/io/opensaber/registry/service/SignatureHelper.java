package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.registry.exception.SignatureException;
import io.opensaber.registry.middleware.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SignatureHelper {
    @Autowired
    private SignatureService signatureService;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${registry.rootEntity.type}")
    private String registryRootEntityType;
    @Value("${signature.keysURL}")
    private String signatureKeyURl;

    /** Signs the entity and returns the entity with signed json appending
     * @param rootNode
     * @return
     * @throws SignatureException.UnreachableException
     * @throws SignatureException.CreationException
     */
    public JsonNode signJson(JsonNode rootNode) throws SignatureException.UnreachableException, SignatureException.CreationException {
        JsonNode signedRoot = rootNode;
        Map signReq = new HashMap<String, Object>();
        signReq.put("entity", rootNode);
        Map<String, Object> signMap = (Map<String, Object>) signatureService.sign(signReq);
        mergeSign(rootNode.get(registryRootEntityType).get(Constants.SIGNATURES_STR), signMap);
        return signedRoot;
    }

    /** Merges sign data to entity json
     * @param signNode
     * @param signMap
     */
    private void mergeSign(JsonNode signNode, Map<String, Object> signMap) {
        ArrayNode parentSignNode = null;
        if(signNode.isArray()){
            parentSignNode = (ArrayNode) signNode;
        } else {
            parentSignNode = JsonNodeFactory.instance.arrayNode();
        }
        Map<String, Object> entitySignMap = new HashMap<>();
        entitySignMap.put(Constants.SIGN_SIGNATURE_VALUE, signMap.get(Constants.SIGN_SIGNATURE_VALUE));
        entitySignMap.put(Constants.SIGN_CREATOR, signatureKeyURl + signMap.get("keyId"));
        entitySignMap.put(Constants.SIGNATURE_FOR, registryRootEntityType);
        entitySignMap.put(Constants.TYPE_STR_JSON_LD, "RSASignature2018");
        entitySignMap.put(Constants.SIGN_CREATED_TIMESTAMP, "");
        entitySignMap.put(Constants.SIGN_NONCE, "");
        JsonNode entitySignNode = objectMapper.convertValue(entitySignMap, JsonNode.class);
        parentSignNode.add(entitySignNode);
    }

}