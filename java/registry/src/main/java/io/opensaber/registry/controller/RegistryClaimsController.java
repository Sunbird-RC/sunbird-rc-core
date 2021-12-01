package io.opensaber.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.actors.factory.PluginRouter;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.helper.RegistryHelper;
import io.opensaber.registry.util.ClaimRequestClient;
import io.opensaber.registry.util.DefinitionsManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletRequest;

@RestController
public class RegistryClaimsController {
    private static final Logger logger = LoggerFactory.getLogger(RegistryClaimsController.class);
    private final ClaimRequestClient claimRequestClient;
    private final RegistryHelper registryHelper;
    private final DefinitionsManager definitionsManager;

    public RegistryClaimsController(ClaimRequestClient claimRequestClient, RegistryHelper registryHelper, DefinitionsManager definitionsManager) {
        this.registryHelper = registryHelper;
        this.claimRequestClient = claimRequestClient;
        this.definitionsManager = definitionsManager;
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
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Fetching claim failed {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(e.getStatusCode());
        } catch (Exception exception) {
            exception.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/claims/{claimId}/attest", method = RequestMethod.POST)
    public ResponseEntity<Object> attestClaim(
            @PathVariable String claimId,
            @PathVariable String entityName,
            @RequestBody ObjectNode requestBody,
            HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        try {
            logger.info("Attesting claim {} as  {}", claimId, entityName);
            JsonNode action = requestBody.get("action");
            ObjectNode additionalInputs = generateAdditionInput(claimId, entityName, requestBody, request, action);

            final String attestorPlugin = "did:internal:ClaimPluginActor";
            PluginRequestMessage pluginRequestMessage = PluginRequestMessage.builder().build();
            pluginRequestMessage.setAttestorPlugin(attestorPlugin);
            pluginRequestMessage.setAdditionalInputs(additionalInputs);
            pluginRequestMessage.setStatus(action.asText());
            PluginRouter.route(pluginRequestMessage);

            responseParams.setStatus(Response.Status.SUCCESSFUL);
            return new ResponseEntity<>(responseParams, HttpStatus.OK);
        } catch (Exception exception) {
            logger.error("Exception : {}", exception.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(exception.getMessage());
            return new ResponseEntity<>(responseParams, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @NotNull
    private ObjectNode generateAdditionInput(String claimId, String entityName, ObjectNode requestBody, HttpServletRequest request, JsonNode action) throws Exception {
        JsonNode notes = requestBody.get("notes");
        logger.info("Action : {} , Notes: {}", action, notes);
        JsonNode result = registryHelper.getRequestedUserDetails(request, entityName);
        JsonNode attestorInfo = result.get(entityName).get(0);
        ObjectNode additionalInputs = JsonNodeFactory.instance.objectNode();
        additionalInputs.set("attestorInfo", attestorInfo);
        additionalInputs.set("action", action);
        additionalInputs.set("notes", notes);
        additionalInputs.put("claimId", claimId);
        return additionalInputs;
    }
}
