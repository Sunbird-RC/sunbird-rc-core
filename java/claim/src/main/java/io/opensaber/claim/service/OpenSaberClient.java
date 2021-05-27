package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.claim.contants.OpensaberApiUrlPaths;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.model.AttestorActions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.opensaber.claim.contants.AttributeNames.*;


@Service
public class OpenSaberClient {
    private final String openSaberUrl;
    RestTemplate restTemplate = new RestTemplate();

    public OpenSaberClient(@Value("${opensaber.url}")String openSaberUrl) {
        this.openSaberUrl = openSaberUrl;
    }

    public AttestationPropertiesDTO getAttestationProperties(Claim claim) throws IOException {
        String url = openSaberUrl + OpensaberApiUrlPaths.ATTESTATION_PROPERTIES
                .replace(ENTITY_ID, claim.getEntityId())
                .replace(ENTITY, claim.getEntity());
        return restTemplate.getForObject(url, AttestationPropertiesDTO.class);
    }

    public void updateAttestedProperty(Claim claim, HttpHeaders headers) {
        String url = openSaberUrl + OpensaberApiUrlPaths.ATTEST
                .replace(PROPERTY_ID, claim.getPropertyId())
                .replace(PROPERTY, claim.getProperty());
        HashMap<String, Object> requestBody = new HashMap<String, Object>() {{
            put(ACTION, AttestorActions.DENIED);
        }};
        HttpEntity<HashMap<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        restTemplate.postForObject(url, entity, Void.class);
    }

    public void updateAttestedProperty(Claim claim, String attestedData, HttpHeaders headers) {
        HashMap<String, Object> requestBody = new HashMap<String, Object>() {{
            put(ACTION, AttestorActions.GRANTED);
            put(ATTESTED_DATA, attestedData);
        }};
        String url = openSaberUrl + OpensaberApiUrlPaths.ATTEST
                .replace(ENTITY_ID, claim.getEntityId())
                .replace(ENTITY, claim.getEntity())
                .replace(PROPERTY_ID, claim.getPropertyId())
                .replace(PROPERTY, claim.getProperty());
        HttpEntity<HashMap<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        restTemplate.postForObject(url, entity, Void.class);
    }
}
