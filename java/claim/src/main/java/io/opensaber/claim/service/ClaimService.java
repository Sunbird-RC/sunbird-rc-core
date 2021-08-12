package io.opensaber.claim.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.claim.dto.ClaimWithNotesDTO;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.entity.ClaimNote;
import io.opensaber.claim.exception.ClaimAlreadyProcessedException;
import io.opensaber.claim.exception.ResourceNotFoundException;
import io.opensaber.claim.exception.UnAuthorizedException;
import io.opensaber.claim.model.ClaimStatus;
import io.opensaber.claim.repository.ClaimNoteRepository;
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

import static io.opensaber.claim.contants.AttributeNames.NOTES;
import static io.opensaber.claim.contants.ErrorMessages.*;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimNoteRepository claimNoteRepository;
    private final OpenSaberClient openSaberClient;
    private final ClaimsAuthorizer claimsAuthorizer;
    private static final Logger logger = LoggerFactory.getLogger(ClaimService.class);

    @Autowired
    public ClaimService(ClaimRepository claimRepository, ClaimNoteRepository claimNoteRepository, OpenSaberClient openSaberClient, ClaimsAuthorizer claimsAuthorizer) {
        this.claimRepository = claimRepository;
        this.claimNoteRepository = claimNoteRepository;
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
        if(request.has(NOTES)) {
            addNotes(request.get(NOTES).asText(), claim);
        }
        claim.setAttestedOn(new Date());
        claim.setStatus(ClaimStatus.CLOSED.name());
        claimRepository.save(claim);
    }

    public void addNotes(String notes, Claim claim) {
        ClaimNote claimNote = new ClaimNote();
        claimNote.setNotes(notes);
        claimNote.setPropertyURI(claim.getPropertyURI());
        claimNote.setEntityId(claim.getEntityId());
        claimNoteRepository.save(claimNote);
    }

    public List<ClaimNote> getClaimWithNotes(Claim claim) {
        return claimNoteRepository.findByEntityIdAndPropertyURI(claim.getEntityId(), claim.getPropertyURI());
    }

    public ClaimWithNotesDTO generateNotesForTheClaim(Claim claim) {
        List<ClaimNote> notes = getClaimWithNotes(claim);
        ClaimWithNotesDTO claimWithNotesDTO = new ClaimWithNotesDTO();
        claimWithNotesDTO.setNotes(notes);
        claimWithNotesDTO.setClaim(claim);
        return claimWithNotesDTO;
    }
}
