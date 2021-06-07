package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.entity.Role;
import io.opensaber.claim.exception.ClaimAlreadyProcessedException;
import io.opensaber.claim.exception.InvalidRoleException;
import io.opensaber.claim.exception.ResourceNotFoundException;
import io.opensaber.claim.model.ClaimStatus;
import io.opensaber.claim.repository.ClaimRepository;
import io.opensaber.claim.repository.RoleRepository;
import io.opensaber.pojos.attestation.AttestationPolicy;
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
    private final RoleRepository roleRepository;
    private static final Logger logger = LoggerFactory.getLogger(ClaimService.class);

    @Autowired
    public ClaimService(ClaimRepository claimRepository, OpenSaberClient openSaberClient, RoleRepository roleRepository) {
        this.claimRepository = claimRepository;
        this.openSaberClient = openSaberClient;
        this.roleRepository = roleRepository;
    }

    public Claim save(Claim claim) {
        roleRepository.saveAll(claim.getRoles());
        return claimRepository.save(claim);
    }

    public Optional<Claim> findById(String id) {
        return claimRepository.findById(id);
    }

    public List<Claim> findAll() {
        return claimRepository.findAll();
    }

    public List<Claim> findClaimsForAttestor(List<String> roles) {
        List<Claim> claims = claimRepository.findByRoles(Role.createRoles(roles));
//        TODO: filter the claims by expression
//        claims.stream().filter(claim -> {
//            String referenceId = claim.getReferenceId();
//            String[] strings = referenceId.split("==");
//            strings[1]
//        })
        // now filter based on refId
        return claims;
    }
    public void updateNotes(String claimId, Optional<String> notes, HttpHeaders headers) {
        logger.info("Initiating denial action for claim with id{} ",  claimId);
        Claim claim = findById(claimId).orElseThrow(() -> new ResourceNotFoundException(CLAIM_NOT_FOUND));
        if(claim.isClosed()) {
            throw new ClaimAlreadyProcessedException(CLAIM_IS_ALREADY_PROCESSED);
        }
        // TODO: read roles from jwt
        List<String> roles = Collections.singletonList("bo");
        if(!claim.isValidRole(roles)) {
            throw new InvalidRoleException(USER_NOT_AUTHORIZED);
        }
        claim.setNotes(notes.orElse(""));
        claim.setStatus(ClaimStatus.CLOSED.name());
        claim.setAttestedOn(new Date());
        save(claim);
        openSaberClient.updateAttestedProperty(claim, headers);
        logger.info("Clam with id {} is successfully denied",  claimId);
    }

    public void grantClaim(String claimId, List<String> role, HttpHeaders header) {
        logger.info("Initiating grant action for claim with id {} ",  claimId);
        Claim claim = findById(claimId).orElseThrow(() -> new ResourceNotFoundException(CLAIM_NOT_FOUND));
        if(claim.isClosed()) {
            throw new ClaimAlreadyProcessedException(CLAIM_IS_ALREADY_PROCESSED);
        }
        AttestationPropertiesDTO attestationProperties = openSaberClient.getAttestationProperties(claim);
        Optional<AttestationPolicy> attestationPolicyOptional = getAttestationPolicy(claim, attestationProperties);
        AttestationPolicy attestationPolicy = attestationPolicyOptional.orElseThrow(() -> new ResourceNotFoundException(ATTESTATION_POLICY_IS_NOT_FOUND));
        logger.info("Found the attestation policy {} ",  attestationPolicy.toString());
        if(!claim.isValidRole(role)) {
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
                .filter(policy -> policy.hasProperty(claim.getProperty()))
                .findFirst();
    }

    public Claim transform(Claim savedClaim) {
        Claim claim = new Claim();
        claim.setAttestedOn(savedClaim.getAttestedOn());
        claim.setCreatedAt(savedClaim.getCreatedAt());
        claim.setProperty(savedClaim.getProperty());
        claim.setPropertyId(savedClaim.getPropertyId());
        claim.setEntity(savedClaim.getEntity());
        claim.setEntityId(savedClaim.getEntityId());
        claim.setStatus(savedClaim.getStatus());
        claim.setNotes(savedClaim.getNotes());
        claim.setId(savedClaim.getId());
        claim.setRoles(getRoles(savedClaim));
        return claim;
    }

    public List<Claim> transform(List<Claim> claims) {
        return claims.stream().map(this::transform).collect(Collectors.toList());
    }
    private List<Role> getRoles(Claim claim) {
        return claim.getRoles()
                .stream()
                .map(role -> new Role(role.getName()))
                .collect(Collectors.toList());
    }
}
