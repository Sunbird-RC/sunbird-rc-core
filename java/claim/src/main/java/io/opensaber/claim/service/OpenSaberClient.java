package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.claim.contants.OpensaberApiUrlPaths;
import io.opensaber.claim.dto.AttestationPropertiesDTO;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static io.opensaber.claim.contants.AttributeNames.*;


@Service
public class OpenSaberClient {
    private static final Logger logger = LoggerFactory.getLogger(OpenSaberClient.class);
    private final String openSaberUrl;
    RestTemplate restTemplate = new RestTemplate();

    public OpenSaberClient(@Value("${opensaber.url}")String openSaberUrl) {
        this.openSaberUrl = openSaberUrl;
    }

    public AttestationPropertiesDTO getAttestationProperties(Claim claim) {
        String url = openSaberUrl + OpensaberApiUrlPaths.ATTESTATION_PROPERTIES
                .replace(ENTITY_ID, claim.getEntityId())
                .replace(ENTITY, claim.getEntity());
        logger.info("Sending request to {}", url);
        return restTemplate.getForObject(url, AttestationPropertiesDTO.class);
    }

    public ResponseEntity<Object> sendAttestationResponseToRequester(Claim claim, JsonNode request) {
        String url = openSaberUrl + OpensaberApiUrlPaths.ATTEST
                .replace(ENTITY_ID, claim.getEntityId())
                .replace(ENTITY, claim.getEntity())
                .replace(PROPERTY_URI, claim.getPropertyURI());
        logger.info("Sending attestation request to {}", url);
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(request), Object.class);
    }

    public JsonNode getEntity(String entity, HttpHeaders headers) {
        String url = openSaberUrl + OpensaberApiUrlPaths.USER_INFO.replace(ENTITY, entity);
        HttpEntity<JsonNode> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, JsonNode.class);
        if(!responseEntity.getStatusCode().is2xxSuccessful()) {
           throw new ResourceNotFoundException("Attestor info is not present in registry");
        }
        return responseEntity.getBody();
    }
}
