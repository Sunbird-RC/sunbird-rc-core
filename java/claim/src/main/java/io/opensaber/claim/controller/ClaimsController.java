package io.opensaber.claim.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.model.AttestationActions;
import io.opensaber.claim.model.ClaimStatus;
import io.opensaber.claim.service.ClaimService;
import io.opensaber.claim.service.OpenSaberClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Controller
public class ClaimsController {

    private final ClaimService claimService;

    @Autowired
    public ClaimsController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @RequestMapping(value = "/api/v1/claims", method = RequestMethod.GET)
    public ResponseEntity<Object> getClaims() throws IOException {
        Object response = claimService.findAll();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/claims", method = RequestMethod.POST)
    public ResponseEntity<Claim> save(@RequestBody Claim claim, @RequestHeader HttpHeaders headers) {
        claim.setStatus(ClaimStatus.getOpen());
        return new ResponseEntity<>(claimService.save(claim), HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/claims/{claimId}/{action}", method = RequestMethod.POST)
    public ResponseEntity<Object> attestClaims(@PathVariable String claimId,
                                               @PathVariable AttestationActions action,
                                               @RequestHeader HttpHeaders headers,
                                               @RequestBody JsonNode requestBody) throws Exception {
        // TODO: fetch role from jwt
        String role = "bo";
        switch (action) {
            case GRANTED:
                claimService.grantClaim(claimId, role, headers, this);
                break;
            case DENIED:
                String notes = "";
                if(requestBody.has("notes")) {
                    notes = requestBody.get("notes").asText();
                }
                claimService.updateNotes(claimId, notes, headers);
                break;
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
