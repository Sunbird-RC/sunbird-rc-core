package io.opensaber.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.pojos.dto.ClaimDTO;
import io.opensaber.registry.controller.RegistryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import java.util.HashMap;

@Component
public class ClaimRequestClient {
    private static Logger logger = LoggerFactory.getLogger(RegistryController.class);
    private final String claimRequestUrl;
    private final RestTemplate restTemplate;
    private static final String CLAIMS_PATH = "/api/v1/claims";
    private static final String FETCH_CLAIMS_PATH = "/api/v1/getClaims";

    ClaimRequestClient(@Value("${claims.url}") String claimRequestUrl, RestTemplate restTemplate) {
        this.claimRequestUrl = claimRequestUrl;
        this.restTemplate = restTemplate;
    }

    public HashMap<String, Object> riseClaimRequest(ClaimDTO claimDTO) {
        HashMap<String, Object> hashMap = restTemplate.postForObject(claimRequestUrl + CLAIMS_PATH, claimDTO, HashMap.class);
        logger.info("Claim has successfully risen {}", hashMap.toString());
        return hashMap;
    }

    public JsonNode getClaims(JsonNode jsonNode, String entityName) {
        ObjectNode requestBody = JsonNodeFactory.instance.objectNode();
        requestBody.set("attestorInfo", jsonNode);
        requestBody.put("entity" ,entityName);
        return restTemplate.postForObject(claimRequestUrl + FETCH_CLAIMS_PATH, requestBody,JsonNode.class);
    }
}
