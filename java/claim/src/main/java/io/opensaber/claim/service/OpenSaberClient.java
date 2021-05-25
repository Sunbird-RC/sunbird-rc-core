package io.opensaber.claim.service;

import io.opensaber.claim.contants.OpensaberApiUrlPaths;
import io.opensaber.claim.entity.Claim;
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
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        restTemplate.postForObject(url, entity, Void.class);
    }

    public void updateAttestedProperty(Claim claim, Map<String, Object> attestedData, HttpHeaders headers) {
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(attestedData, headers);
        String url = openSaberUrl + OpensaberApiUrlPaths.ATTEST_PROPERTY
                .replace("PROPERTY", claim.getProperty())
                .replace("PROPERTY_ID", claim.getPropertyId());

        restTemplate.postForObject(url, entity, Void.class);
    }
}
