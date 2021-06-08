package io.opensaber.registry.util;

import io.opensaber.pojos.dto.ClaimDTO;
import io.opensaber.registry.controller.RegistryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

@Component
public class ClaimRequestClient {
    private static Logger logger = LoggerFactory.getLogger(RegistryController.class);
    private final String claimRequestUrl;
    private final RestTemplate restTemplate;

    ClaimRequestClient(@Value("${claims.url}") String claimRequestUrl, RestTemplate restTemplate) {
        this.claimRequestUrl = claimRequestUrl;
        this.restTemplate = restTemplate;
    }

    public HashMap<String, Object> riseClaimRequest(String entityName, String entityId, String property, String propertyId, String conditions) throws Exception {
        ClaimDTO claimDTO = new ClaimDTO();
        claimDTO.setEntity(entityName);
        claimDTO.setEntityId(entityId);
        claimDTO.setProperty(property);
        claimDTO.setPropertyId(propertyId);
        claimDTO.setConditions(conditions);
        String claimsPath = "/api/v1/claims";
        HashMap<String, Object> hashMap = restTemplate.postForObject(claimRequestUrl + claimsPath, claimDTO, HashMap.class);
        logger.info("Claim has successfully risen {}", hashMap.toString());
        return hashMap;
    }

}
