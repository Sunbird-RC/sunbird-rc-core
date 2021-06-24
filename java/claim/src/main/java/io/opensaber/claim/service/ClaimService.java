package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.exception.ClaimAlreadyProcessedException;
import io.opensaber.claim.exception.UnAuthorizedException;
import io.opensaber.claim.exception.ResourceNotFoundException;
import io.opensaber.claim.model.ClaimStatus;
import io.opensaber.claim.repository.ClaimRepository;
import io.opensaber.pojos.attestation.AttestationPolicy;
import io.opensaber.registry.middleware.service.ConditionResolverService;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static io.opensaber.claim.contants.AttributeNames.PROPERTY_ID;
import static io.opensaber.claim.contants.ErrorMessages.*;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final OpenSaberClient openSaberClient;
    private final ConditionResolverService conditionResolverService;
    private static final Logger logger = LoggerFactory.getLogger(ClaimService.class);

    @Autowired
    public ClaimService(ClaimRepository claimRepository, OpenSaberClient openSaberClient, ConditionResolverService conditionResolverService) {
        this.claimRepository = claimRepository;
        this.openSaberClient = openSaberClient;
        this.conditionResolverService = conditionResolverService;
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

    public List<Claim> findClaimsForAttestor(String entity, JsonNode attestorNode) {
        List<Claim> claims = claimRepository.findByAttestorEntity(entity);
        return claims.stream().filter(claim -> {
            String ATTESTOR = "ATTESTOR";
            String resolvedCondition = conditionResolverService.resolve(attestorNode,
                    ATTESTOR, claim.getConditions(), Collections.emptyList());
            return conditionResolverService.evaluate(resolvedCondition);
        }).collect(Collectors.toList());
    }

    public void updateNotes(String claimId, Optional<String> notes, HttpHeaders headers, List<String> conditions) {
        logger.info("Initiating denial action for claim with id{} ",  claimId);
        Claim claim = findById(claimId).orElseThrow(() -> new ResourceNotFoundException(CLAIM_NOT_FOUND));
        if(claim.isClosed()) {
            throw new ClaimAlreadyProcessedException(CLAIM_IS_ALREADY_PROCESSED);
        }
        if(!conditions.contains(claim.getAttestorEntity())) {
            throw new UnAuthorizedException(USER_NOT_AUTHORIZED);
        }
        claim.setNotes(notes.orElse(""));
        claim.setStatus(ClaimStatus.CLOSED.name());
        claim.setAttestedOn(new Date());
        save(claim);
        openSaberClient.updateAttestedProperty(claim, headers);
        logger.info("Clam with id {} is successfully denied",  claimId);
    }

    public void grantClaim(String claimId, List<String> conditions, HttpHeaders header) {
        logger.info("Initiating grant action for claim with id {} ",  claimId);
        Claim claim = findById(claimId).orElseThrow(() -> new ResourceNotFoundException(CLAIM_NOT_FOUND));
        if(claim.isClosed()) {
            throw new ClaimAlreadyProcessedException(CLAIM_IS_ALREADY_PROCESSED);
        }
        if(!conditions.contains(claim.getAttestorEntity())) {
            throw new UnAuthorizedException(USER_NOT_AUTHORIZED);
        }
        AttestationPropertiesDTO attestationProperties = openSaberClient.getAttestationProperties(claim);
        Optional<AttestationPolicy> attestationPolicyOptional = getAttestationPolicy(claim, attestationProperties);
        AttestationPolicy attestationPolicy = attestationPolicyOptional.orElseThrow(() -> new ResourceNotFoundException(ATTESTATION_POLICY_IS_NOT_FOUND));
        logger.info("Found the attestation policy {} ",  attestationPolicy.toString());
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
                .filter(policy -> policy.hasProperty(claim.getProperty()))
                .findFirst();
    }
}
