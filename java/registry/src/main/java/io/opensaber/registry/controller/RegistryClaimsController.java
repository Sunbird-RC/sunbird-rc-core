package io.opensaber.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.helper.RegistryHelper;
import io.opensaber.registry.util.ClaimRequestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.json.Json;
import javax.servlet.http.HttpServletRequest;

@RestController
public class RegistryClaimsController {
    private static Logger logger = LoggerFactory.getLogger(RegistryClaimsController.class);
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
}
