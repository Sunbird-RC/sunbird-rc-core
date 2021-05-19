package io.opensaber.registry.util;

import io.opensaber.registry.controller.RegistryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class ClaimRequestClient {
    private static Logger logger = LoggerFactory.getLogger(RegistryController.class);
    private final String claimRequestUrl;
    private final RestTemplate restTemplate;

    ClaimRequestClient(@Value("{claims.url}") String claimRequestUrl, RestTemplate restTemplate) {
        this.claimRequestUrl = claimRequestUrl;
        this.restTemplate = restTemplate;
    }

    public void riseClaimRequest(String entityName, String entityId, String property, String propertyId) throws Exception {
        Map<String, Object> claimDetails = new HashMap<String, Object>(){{
            put("entity", entityName);
            put("entityId", entityId);
            put("property", property);
            put("propertyId", propertyId);
        }};
        String claimsPath = "/api/v1/claims";
        Object hashMap = restTemplate.postForObject(claimRequestUrl + claimsPath, claimDetails, HashMap.class);
        logger.info("Claim has successfully risen ", hashMap.toString());
    }

}
