package io.opensaber.claim.controller;

import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.service.ClaimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
    public ResponseEntity<Claim> save(@RequestBody Claim claim) {
        return new ResponseEntity<>(claimService.save(claim), HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/claims/{claimId}/{action}", method = RequestMethod.POST)
    public ResponseEntity<Object> attestClaims(@PathVariable String claimId, @PathVariable String action) {
        // read particular claim by id
        // fetch the entity, property and their ids
        // validate whether the user can do the attestation by reading attestation policy for the given entity & property
        // Then create attested data based on the policy.
        // Fire API to add it as entity but where?
        // where to add attested data,  what is the format?
        //TODO: use enum for action
        return null;
    }
}
