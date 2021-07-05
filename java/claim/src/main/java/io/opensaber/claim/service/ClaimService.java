package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.exception.ClaimAlreadyProcessedException;
import io.opensaber.claim.exception.ResourceNotFoundException;
import io.opensaber.claim.exception.UnAuthorizedException;
import io.opensaber.claim.repository.ClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.opensaber.claim.contants.ErrorMessages.*;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final OpenSaberClient openSaberClient;
    private final ClaimsAuthorizer claimsAuthorizer;
    private static final Logger logger = LoggerFactory.getLogger(ClaimService.class);

    @Autowired
    public ClaimService(ClaimRepository claimRepository, OpenSaberClient openSaberClient, ClaimsAuthorizer claimsAuthorizer) {
        this.claimRepository = claimRepository;
        this.openSaberClient = openSaberClient;
        this.claimsAuthorizer = claimsAuthorizer;
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
        logger.info("Found {} claims to process", claims.size());
        return claims.stream()
                .filter(claim -> claimsAuthorizer.isAuthorized(claim, attestorNode))
                .collect(Collectors.toList());
    }

    public ResponseEntity<Object> attestClaim(String claimId, JsonNode attestorNode, JsonNode request) {
        Claim claim = findById(claimId).orElseThrow(() -> new ResourceNotFoundException(CLAIM_NOT_FOUND));
        logger.info("Processing claim {}", claim.toString());
        if (claim.isClosed()) {
            throw new ClaimAlreadyProcessedException(CLAIM_IS_ALREADY_PROCESSED);
        }
        if (!claimsAuthorizer.isAuthorized(claim, attestorNode)) {
            throw new UnAuthorizedException(USER_NOT_AUTHORIZED);
        }
        updateClaim(request, claim);
        return openSaberClient.sendAttestationResponseToRequester(claim, request);
    }

    private void updateClaim(JsonNode request, Claim claim) {
        if(request.has("notes")) {
            claim.setNotes(request.get("notes").asText());
        }
        claim.setAttestedOn(new Date());
        claim.setStatus("CLOSED");
        claimRepository.save(claim);
    }
}
