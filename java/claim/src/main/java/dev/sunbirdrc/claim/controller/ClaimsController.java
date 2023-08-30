package dev.sunbirdrc.claim.controller;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.claim.dto.ClaimWithNotesDTO;
import dev.sunbirdrc.claim.entity.Claim;
import dev.sunbirdrc.claim.service.ClaimService;
import dev.sunbirdrc.claim.service.ClaimsAuthorizer;
import dev.sunbirdrc.pojos.dto.ClaimDTO;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.sunbirdrc.claim.contants.AttributeNames.*;

@Controller
public class ClaimsController {

    private final ClaimService claimService;
    private final ClaimsAuthorizer claimsAuthorizer;
    private static final Logger logger = LoggerFactory.getLogger(ClaimsController.class);

    @Autowired
    public ClaimsController(ClaimService claimService, ClaimsAuthorizer claimsAuthorizer) {
        this.claimService = claimService;
        this.claimsAuthorizer = claimsAuthorizer;
    }

    @RequestMapping(value = "/api/v1/getClaims", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> getClaims(@RequestHeader HttpHeaders headers,
                                                @RequestBody JsonNode requestBody, Pageable pageable) {
        String entity = requestBody.get(LOWERCASE_ENTITY).asText();
        JsonNode attestorNode = requestBody.get(ATTESTOR_INFO);
        Map<String, Object> claims = claimService.findClaimsForAttestor(entity, attestorNode, pageable);
        return new ResponseEntity<>(claims, HttpStatus.OK);
    }


    @RequestMapping(value = "/api/v2/getClaims", method = RequestMethod.POST)
    public ResponseEntity<List<Claim>> getStudentClaims(@RequestHeader HttpHeaders headers,
                                                         @RequestBody JsonNode requestBody, Pageable pageable) {
        String entity = requestBody.get(LOWERCASE_ENTITY).asText();
        JsonNode attestorNode = requestBody.get(ATTESTOR_INFO);
        List<Claim> claims = claimService.findByRequestorName(attestorNode.asText(), pageable);
        return new ResponseEntity<>(claims, HttpStatus.OK);
    }
    @RequestMapping(value = "/api/v1/getClaims/{claimId}", method = RequestMethod.POST)
    public ResponseEntity<ClaimWithNotesDTO> getClaimById(@RequestHeader HttpHeaders headers, @PathVariable String claimId,
                                              @RequestBody JsonNode requestBody) {
        JsonNode attestorNode = requestBody.get(ATTESTOR_INFO);
        Optional<Claim> claim = claimService.findById(claimId);
        if (!claim.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (claimsAuthorizer.isAuthorizedRequestor(claim.get(), attestorNode) ||
                claimsAuthorizer.isAuthorizedAttestor(claim.get(), attestorNode)) {
            ClaimWithNotesDTO claimWithNotesDTO = claimService.generateNotesForTheClaim(claim.get());
            return new ResponseEntity<>(claimWithNotesDTO, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/api/v1/claims", method = RequestMethod.POST)
    public ResponseEntity<Claim> save(@RequestBody ClaimDTO claimDTO) {
        JSONObject jsonObject = new JSONObject(claimDTO.getPropertyData());
        String credType1 = jsonObject.get("credType").toString();
        claimDTO.setCredtype(credType1);
        logger.info("Adding new claimDTO {} ", claimDTO.toString());
        logger.info("Cred Type new claimDTO {} ", claimDTO.getCredtype());
        Claim savedClaim = claimService.save(Claim.fromDTO(claimDTO));
        claimService.addNotes(claimDTO.getNotes(), savedClaim, claimDTO.getRequestorName());
        return new ResponseEntity<>(savedClaim, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/claims/{claimId}", method = RequestMethod.POST)
    public ResponseEntity<Claim> attestClaims(@PathVariable String claimId, @RequestBody JsonNode requestBody) {
        logger.info("Attesting claim : {}", claimId);
        Claim updatedClaim = claimService.attestClaim(claimId, requestBody);
        return new ResponseEntity<>(updatedClaim, HttpStatus.OK);
    }

    @GetMapping(value = "/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok().build();
    }

}
