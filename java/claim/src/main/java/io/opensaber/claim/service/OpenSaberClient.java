package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.claim.contants.OpensaberApiUrlPaths;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.model.AttestationActions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Service
public class OpenSaberClient {
    private final String openSaberUrl;
    RestTemplate restTemplate = new RestTemplate();

    public OpenSaberClient(@Value("${opensaber.url}")String openSaberUrl) {
        this.openSaberUrl = openSaberUrl;
    }

    public HashMap<String, Object> getAttestationProperties(Claim claim) throws IOException {
        String url = openSaberUrl + OpensaberApiUrlPaths.ATTESTATION_PROPERTIES
                .replace("ENTITY", claim.getEntity())
                .replace("ENTITY_ID", claim.getEntityId());
        return restTemplate.getForObject(url, HashMap.class);
    }

    public void updateAttestedProperty(Claim claim, HttpHeaders headers) {
        String url = openSaberUrl + OpensaberApiUrlPaths.ATTEST_PROPERTY
                .replace("PROPERTY", claim.getProperty())
                .replace("PROPERTY_ID", claim.getPropertyId());
        HashMap<String, Object> requestBody = new HashMap<String, Object>() {{
            put("action", AttestationActions.DENIED);
        }};
        HttpEntity<HashMap<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        restTemplate.postForObject(url, entity, Void.class);
    }

    public void updateAttestedProperty(Claim claim, Map<String, Object> attestedData, HttpHeaders headers) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.convertValue(attestedData, JsonNode.class);
        HashMap<String, Object> requestBody = new HashMap<String, Object>() {{
            put("action", AttestationActions.GRANTED);
            put("attestedData", node.toString());
        }};
        String url = openSaberUrl + OpensaberApiUrlPaths.ATTEST_PROPERTY
                .replace("PROPERTY", claim.getProperty())
                .replace("PROPERTY_ID", claim.getPropertyId());

        restTemplate.postForObject(url, requestBody, Void.class);
    }
}
