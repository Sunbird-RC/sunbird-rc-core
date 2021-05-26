package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.model.ClaimStatus;
import io.opensaber.claim.repository.ClaimRepository;
import io.opensaber.pojos.attestation.AttestationPolicy;
import net.minidev.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import java.util.*;

import static io.opensaber.claim.contants.AttributeNames.PROPERTY_ID;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final OpenSaberClient openSaberClient;

    @Autowired
    public ClaimService(ClaimRepository claimRepository, OpenSaberClient openSaberClient) {
        this.claimRepository = claimRepository;
        this.openSaberClient = openSaberClient;
    }

    public Claim save(Claim claim) {
        return claimRepository.save(claim);
    }

    public Optional<Claim> findById(String id) {
        return claimRepository.findById(id);
    }

    public List<Claim> findAll() {
        return claimRepository.findAll();
    }

    public void updateNotes(String claimId, Optional<String> notes, HttpHeaders headers) {
        Optional<Claim> claimOptional = findById(claimId);
        if(claimOptional.isPresent()) {
            Claim claim = claimOptional.get();
            claim.setNotes(notes.orElse(""));
            claim.setStatus(ClaimStatus.CLOSED.name());
            claim.setAttestedOn(new Date());
            save(claim);
            openSaberClient.updateAttestedProperty(claim, headers);
        }
    }

    public void grantClaim(String claimId, String role, HttpHeaders header) throws Exception {
        Optional<Claim> claimOptional = findById(claimId);
        if(claimOptional.isPresent()) {
            Claim claim = claimOptional.get();
            AttestationPropertiesDTO attestationProperties = openSaberClient.getAttestationProperties(claim);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode entityNode = objectMapper.convertValue(attestationProperties.getEntity(), JsonNode.class);
            AttestationPolicy attestationPolicy = getAttestationPolicy(claim, attestationProperties);
            if(attestationPolicy == null){
                throw new Exception("Attestation policy is not defined");
            }
            if(!attestationPolicy.isValidRole(role)) {
                throw new Exception("Invalid role, See ya!!!");
            }
            Map<String, Object> attestedData = generateAttestedData(claim, entityNode, attestationPolicy);
            claim.setStatus(ClaimStatus.CLOSED.name());
            claim.setAttestedOn(new Date());
            save(claim);
            openSaberClient.updateAttestedProperty(claim, attestedData, header);
        }
    }

    private Map<String, Object> generateAttestedData(Claim claim, JsonNode entityNode, AttestationPolicy attestationPolicy) {
        Map<String, Object> attestedData = new HashMap<>();
        for (String path: attestationPolicy.getPaths()) {
            if(path.contains(PROPERTY_ID)) {
                path = path.replace(PROPERTY_ID, claim.getPropertyId());
            }
            DocumentContext context = JsonPath.parse(entityNode.toString());
            Object result = context.read(path);
            if(result.getClass().equals(JSONArray.class)) {
                HashMap<String, Object> extractedVal = (HashMap) ((JSONArray) result).get(0);
                attestedData.putAll(extractedVal);
            } else {
                attestedData.putAll((HashMap)result);
            }
        }
        return attestedData;
    }

    private AttestationPolicy getAttestationPolicy(Claim claim, AttestationPropertiesDTO attestationProperties) {
        return attestationProperties.getAttestationPolicies()
                .stream()
                .filter(policy -> policy.getProperty().equals(claim.getProperty()))
                .findFirst()
                .orElse(null);
    }
}
