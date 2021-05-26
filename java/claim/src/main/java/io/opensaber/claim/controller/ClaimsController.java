package io.opensaber.claim.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.model.AttestorActions;
import io.opensaber.claim.model.ClaimStatus;
import io.opensaber.claim.service.ClaimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static io.opensaber.claim.contants.AttributeNames.ACTION;
import static io.opensaber.claim.contants.AttributeNames.NOTES;

@Controller
public class ClaimsController {

    private final ClaimService claimService;

    @Autowired
    public ClaimsController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @RequestMapping(value = "/api/v1/claims", method = RequestMethod.GET)
    public ResponseEntity<List<Claim>> getClaims() throws IOException {
        List<Claim> claims = claimService.findAll();
        return new ResponseEntity<>(claims, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/claims", method = RequestMethod.POST)
    public ResponseEntity<Claim> save(@RequestBody Claim claim, @RequestHeader HttpHeaders headers) {
        claim.setStatus(ClaimStatus.OPEN.name());
        return new ResponseEntity<>(claimService.save(claim), HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/claims/{claimId}", method = RequestMethod.POST)
    public ResponseEntity<Object> attestClaims(@PathVariable String claimId,
                                               @RequestHeader HttpHeaders headers,
                                               @RequestBody JsonNode requestBody) throws Exception {
        // TODO: fetch role from jwt
        String role = "bo";
        AttestorActions action = AttestorActions.valueOf(requestBody.get(ACTION).asText());
        switch (action) {
            case GRANTED:
                claimService.grantClaim(claimId, role, headers);
                break;
            case DENIED:
                Optional<String> notes = Optional.empty();
                if(requestBody.has(NOTES)) {
                    notes = Optional.of(requestBody.get(NOTES).asText());
                }
                claimService.updateNotes(claimId, notes, headers);
                break;
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
