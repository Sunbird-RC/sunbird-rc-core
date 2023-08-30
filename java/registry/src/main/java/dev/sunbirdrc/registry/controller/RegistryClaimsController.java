package dev.sunbirdrc.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.actors.factory.PluginRouter;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.Response;
import dev.sunbirdrc.pojos.ResponseParams;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.model.dto.AttestationRequest;
import dev.sunbirdrc.registry.util.ClaimRequestClient;
import dev.sunbirdrc.registry.util.CommonUtils;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.sunbirdrc.registry.middleware.util.Constants.USER_ID;

@RestController
public class RegistryClaimsController extends AbstractController{
    private static final Logger logger = LoggerFactory.getLogger(RegistryClaimsController.class);
    private final ClaimRequestClient claimRequestClient;
    private final RegistryHelper registryHelper;

    public RegistryClaimsController(ClaimRequestClient claimRequestClient,
                                    RegistryHelper registryHelper,
                                    IDefinitionsManager definitionsManager) {
        this.registryHelper = registryHelper;
        this.claimRequestClient = claimRequestClient;
        this.definitionsManager = definitionsManager;
    }

    @RequestMapping(value = "/api/v1/{entityName}/claims", method = RequestMethod.GET)
    public ResponseEntity<Object> getAllClaims(@PathVariable String entityName, Pageable pageable,
                                               HttpServletRequest request) {
        try {
            JsonNode result = registryHelper.getRequestedUserDetails(request, entityName);
            JsonNode claims = claimRequestClient.getClaims(result.get(entityName).get(0), pageable, entityName);
            logger.info("Received {} claims", claims.size());
            return new ResponseEntity<>(claims, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Fetching claims failed {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @RequestMapping(value = "/api/v2/{entityName}/claims", method = RequestMethod.GET)
    public ResponseEntity<Object> getStudentsClaims(@PathVariable String entityName, Pageable pageable,
                                               HttpServletRequest request) {
        List<String> entityList = CommonUtils.getEntityName();
        ResponseEntity<Object> objectResponseEntity = null;
        List list = new ArrayList();
        JsonNode claims = null;
        try {
            for (String entityName1:entityList) {
                JsonNode result = registryHelper.getRequestedUserDetails(request, entityName1);
                if(result!=null) {
                    JsonNode jsonNode = result.get(entityName1);
                    if(jsonNode!=null && jsonNode.size()>0) {
                        JsonNode email = jsonNode.get(0).get("email");
                        if(email!=null) {
                            claims = claimRequestClient.getStudentsClaims(email, pageable, entityName1);
                            break;
                        }
                        logger.info("Received {} claims", claims.size());
                    }
                }
            }
            objectResponseEntity = new ResponseEntity<>(claims, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Fetching claims failed {}", e.getMessage());
            e.printStackTrace();

        }
        return objectResponseEntity;
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
            pluginRequestMessage.setUserId(registryHelper.getKeycloakUserId(request));
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
       // additionalInputs.set("credType", entityName);
        additionalInputs.put("claimId", claimId);
        return additionalInputs;
    }

    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST},value = "/api/v1/send")
    public ResponseEntity<Object> riseAttestation(HttpServletRequest request, @RequestBody AttestationRequest attestationRequest)  {
        try {
            registryHelper.authorize(attestationRequest.getEntityName(), attestationRequest.getEntityId(), request);
        } catch (Exception e) {
            logger.error("Unauthorized exception {}", e.getMessage());
            return createUnauthorizedExceptionResponse(e);
        }
        AttestationPolicy attestationPolicy = registryHelper.getAttestationPolicy(attestationRequest.getEntityName(), attestationRequest.getName());
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEND, "OK", responseParams);

        try {
            // Generate property Data
            String userId = registryHelper.getUserId(request, attestationRequest.getEntityName());
            String emailId = registryHelper.fetchEmailIdFromToken(request, attestationRequest.getEntityName());
            JsonNode entityNode = registryHelper.readEntity(userId, attestationRequest.getEntityName(),
                            attestationRequest.getEntityId(), false, null, false)
                    .get(attestationRequest.getEntityName());
            JsonNode propertyData = JSONUtil.extractPropertyDataFromEntity(entityNode, attestationPolicy.getAttestationProperties(), attestationRequest.getPropertiesOSID());
            if(!propertyData.isNull()) {
                attestationRequest.setPropertyData(propertyData);
            }
            attestationRequest.setUserId(userId);
            attestationRequest.setEmailId(emailId);
            attestationRequest.setCredType(request.getHeader("credType"));
            String attestationOSID = registryHelper.triggerAttestation(attestationRequest, attestationPolicy);
            response.setResult(Collections.singletonMap("attestationOSID", attestationOSID));
        } catch (Exception exception) {
            logger.error("Exception occurred while saving attestation data {}", exception.getMessage());
            exception.printStackTrace();
            responseParams.setErrmsg(exception.getMessage());
            response = new Response(Response.API_ID.SEND, HttpStatus.INTERNAL_SERVER_ERROR.toString(), responseParams);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


}
