package io.opensaber.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.registry.helper.RegistryHelper;
import io.opensaber.registry.util.ClaimRequestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
public class RegistryClaimsController {
    private static final Logger logger = LoggerFactory.getLogger(RegistryClaimsController.class);
    private final ClaimRequestClient claimRequestClient;
    private final RegistryHelper registryHelper;

    public RegistryClaimsController(ClaimRequestClient claimRequestClient, RegistryHelper registryHelper) {
        this.registryHelper = registryHelper;
        this.claimRequestClient = claimRequestClient;
    }

    @RequestMapping(value = "/api/v1/{entityName}/claims", method = RequestMethod.GET)
    public ResponseEntity<Object> getAllClaims(@PathVariable String entityName,
                                               HttpServletRequest request) {
        try {
            JsonNode result = registryHelper.getRequestedUserDetails(request, entityName);
            JsonNode claims = claimRequestClient.getClaims(result.get(entityName).get(0), entityName);
            logger.info("Received {} claims", claims.size());
            return new ResponseEntity<>(claims, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Fetching claims failed {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/claims/{claimId}", method = RequestMethod.GET)
    public ResponseEntity<Object> getClaim(@PathVariable String entityName, @PathVariable String claimId,
                                           HttpServletRequest request) {
        try {
            JsonNode result = registryHelper.getRequestedUserDetails(request, entityName);
            JsonNode claim = claimRequestClient.getClaim(result.get(entityName).get(0), entityName, claimId);
            return new ResponseEntity<>(claim, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Fetching claim failed {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/claims/{claimId}/attest", method = RequestMethod.POST)
    public ResponseEntity<Object> attestClaim(
            @PathVariable String claimId,
            @PathVariable String entityName,
            @RequestBody ObjectNode requestBody,
            HttpServletRequest request) {
        logger.info("Attesting claim {} as  {}", claimId, entityName);
        JsonNode action = requestBody.get("action");
        JsonNode notes = requestBody.get("notes");
        logger.info("Action : {} , Notes: {}", action, notes);
        try {
            JsonNode result = registryHelper.getRequestedUserDetails(request, entityName);
            JsonNode attestorInfo = result.get(entityName).get(0);
            ObjectNode attestRequest = JsonNodeFactory.instance.objectNode();
            attestRequest.set("attestorInfo", attestorInfo);
            attestRequest.set("action", action);
            attestRequest.set("notes", notes);
            return claimRequestClient.attestClaim(
                    attestRequest,
                    claimId
            );
        } catch (Exception exception) {
            logger.error("Exception : {}", exception.getMessage());
            return new ResponseEntity<>("Error sending attestation request to Claims service", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
