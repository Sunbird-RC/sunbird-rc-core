package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.exception.InvalidRoleException;
import io.opensaber.claim.exception.ResourceNotFoundException;
import io.opensaber.claim.model.ClaimStatus;
import io.opensaber.claim.repository.ClaimRepository;
import io.opensaber.pojos.attestation.AttestationPolicy;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import java.util.*;

import static io.opensaber.claim.contants.AttributeNames.PROPERTY_ID;
import static io.opensaber.claim.contants.ErrorMessages.*;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final OpenSaberClient openSaberClient;
    private static final Logger logger = LoggerFactory.getLogger(ClaimService.class);

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
        logger.info("Initiating denial action for claim with id{} ",  claimId);
        Claim claim = findById(claimId).orElseThrow(() -> new ResourceNotFoundException(CLAIM_NOT_FOUND));
        claim.setNotes(notes.orElse(""));
        claim.setStatus(ClaimStatus.CLOSED.name());
        claim.setAttestedOn(new Date());
        save(claim);
        openSaberClient.updateAttestedProperty(claim, headers);
        logger.info("Clam with id {} is successfully denied",  claimId);
    }

    public void grantClaim(String claimId, String role, HttpHeaders header) throws Exception {
        logger.info("Initiating grant action for claim with id {} ",  claimId);
        Claim claim = findById(claimId).orElseThrow(() -> new ResourceNotFoundException(CLAIM_NOT_FOUND));
        AttestationPropertiesDTO attestationProperties = openSaberClient.getAttestationProperties(claim);
        Optional<AttestationPolicy> attestationPolicyOptional = getAttestationPolicy(claim, attestationProperties);
        AttestationPolicy attestationPolicy = attestationPolicyOptional.orElseThrow(() -> new ResourceNotFoundException(ATTESTATION_POLICY_IS_NOT_FOUND));
        logger.info("Found the attestation policy {} ",  attestationPolicy.toString());
        if(!attestationPolicy.isValidRole(role)) {
            throw new InvalidRoleException(USER_NOT_AUTHORIZED);
        }
        Map<String, Object> attestedData = generateAttestedData(attestationProperties.getEntityAsJsonNode(), attestationPolicy, claim.getPropertyId());
        claim.setStatus(ClaimStatus.CLOSED.name());
        claim.setAttestedOn(new Date());
        save(claim);
        JsonNode node = new ObjectMapper().convertValue(attestedData, JsonNode.class);
        openSaberClient.updateAttestedProperty(claim, node.toString(), header);
        logger.info("Clam with id {} is successfully granted",  claimId);
    }

    private Map<String, Object> generateAttestedData(JsonNode entityNode, AttestationPolicy attestationPolicy, String propertyId) {
        Map<String, Object> attestedData = new HashMap<>();
        for (String path: attestationPolicy.getPaths()) {
            if(path.contains(PROPERTY_ID)) {
                path = path.replace(PROPERTY_ID, propertyId);
            }
            DocumentContext context = JsonPath.parse(entityNode.toString());
            Object result = context.read(path);
            if(result.getClass().equals(JSONArray.class)) {
                HashMap<String, Object> extractedVal = (HashMap) ((JSONArray) result).get(0);
                attestedData.putAll(extractedVal);
            } else if(result.getClass().equals(LinkedHashMap.class)) {
                attestedData.putAll((HashMap) result);
            }
            else {
                // It means it is just a value,
                attestedData.putAll(
                        new HashMap<String, Object>(){{
                            put(attestationPolicy.getProperty(), result);
                        }}
                );
            }
        }
        return attestedData;
    }

    private Optional<AttestationPolicy> getAttestationPolicy(Claim claim, AttestationPropertiesDTO attestationProperties) {
        return attestationProperties.getAttestationPolicies()
                .stream()
                .filter(policy -> policy.getProperty().equals(claim.getProperty()))
                .findFirst();
    }
}
