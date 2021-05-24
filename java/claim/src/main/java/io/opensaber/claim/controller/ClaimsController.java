package io.opensaber.claim.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.opensaber.claim.entity.Claim;
import io.opensaber.claim.service.ClaimService;
import io.opensaber.claim.service.OpenSaberClient;
import io.opensaber.pojos.attestation.AttestationPolicy;
import net.minidev.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Controller
public class ClaimsController {

    private final ClaimService claimService;
    private final OpenSaberClient openSaberClient;

    @Autowired
    public ClaimsController(ClaimService claimService, OpenSaberClient openSaberClient) {
        this.claimService = claimService;
        this.openSaberClient = openSaberClient;
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
    public ResponseEntity<Object> attestClaims(@PathVariable String claimId,
                                               @PathVariable AttestationActions action,
                                               @RequestBody JsonNode requestBody) throws Exception {
        // TODO: fetch role from keycloak
        String role = "bo";
        switch (action) {
            case GRANTED:
                Map<String, Object> ans = grantClaim(claimId, role);
                // make a call to save the status & attested data
                break;
            case DENIED:
                if(requestBody.has("notes")) {
                    String notes = requestBody.get("notes").asText();
                }
                break;
        }

        // read particular claim by id
        // fetch the entity, property and their ids
        // validate whether the user can do the attestation by reading attestation policy for the given entity & property
        // Then create attested data based on the policy.
        // Fire API to add it as entity but where?
        // where to add attested data,  what is the format?
        //TODO: use enum for action
        return null;
    }

    private Map<String, Object> grantClaim(String claimId, String role) throws Exception {
        Map<String, Object> ans = new HashMap<String, Object>();
        Optional<Claim> claimOptional = claimService.findById(claimId);
        if(claimOptional.isPresent()) {
            Claim claim = claimOptional.get();
            Map<String, Object> attestationProperties = openSaberClient.getEntity(claim);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode entityNode = objectMapper.convertValue(attestationProperties.get("entity"), JsonNode.class);
            AttestationPolicy attestationPolicy = ((ArrayList<AttestationPolicy>)attestationProperties.get("policy"))
                    .stream()
                    .filter(a -> a.getProperty().equals(claim.getProperty()))
                    .findFirst()
                    .orElse(null);
            if(attestationPolicy == null){
                throw new Exception("Attestation policy is not defined");
            }
            if(!attestationPolicy.getRole().equals(role)) {
                throw new Exception("Invalid role, See ya!!!");
            }
            for (String path: attestationPolicy.getPaths()) {
                if(path.contains("PROPERTY_ID")) {
                    path = path.replace("PROPERTY_ID", claim.getPropertyId());
                }
                DocumentContext context = JsonPath.parse(entityNode.toString());
                Object result = context.read(path);
                if(result.getClass().equals(JSONArray.class)) {
                    HashMap<String, Object> extractedVal = (HashMap) ((JSONArray) result).get(0);
                    ans.putAll(extractedVal);
                } else {
                    ans.putAll((HashMap)result);
                }
            }
            // now answer will have the attested string
        }
        return ans;
    }
}
