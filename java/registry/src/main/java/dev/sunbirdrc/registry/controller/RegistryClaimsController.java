package dev.sunbirdrc.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.actors.factory.PluginRouter;
import dev.sunbirdrc.pojos.PluginRequestMessage;
import dev.sunbirdrc.pojos.PluginRequestMessageCreator;
import dev.sunbirdrc.pojos.Response;
import dev.sunbirdrc.pojos.ResponseParams;
import dev.sunbirdrc.pojos.attestation.Action;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.middleware.service.ConditionResolverService;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.service.FileStorageService;
import dev.sunbirdrc.registry.util.ClaimRequestClient;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import io.minio.errors.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class RegistryClaimsController extends AbstractController{
    private static final Logger logger = LoggerFactory.getLogger(RegistryClaimsController.class);
    private final ClaimRequestClient claimRequestClient;
    private final RegistryHelper registryHelper;
    private final DefinitionsManager definitionsManager;
    private final ConditionResolverService conditionResolverService;
    private final FileStorageService fileStorageService;

    public RegistryClaimsController(ClaimRequestClient claimRequestClient,
                                    RegistryHelper registryHelper,
                                    DefinitionsManager definitionsManager,
                                    ConditionResolverService conditionResolverService,
                                    FileStorageService fileStorageService) {
        this.registryHelper = registryHelper;
        this.claimRequestClient = claimRequestClient;
        this.definitionsManager = definitionsManager;
        this.conditionResolverService = conditionResolverService;
        this.fileStorageService = fileStorageService;
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

    @RequestMapping(value = "/api/v1/send")
    public ResponseEntity<Object> riseAttestation(HttpServletRequest request, @RequestBody JsonNode requestBody)  {
        String entityName = requestBody.get("entityName").asText();
        String entityId = requestBody.get("entityId").asText();
        String attestationName = requestBody.get("name").asText();
        JsonNode additionalInput = requestBody.get("additionalInput");
        try {
            registryHelper.authorize(entityName, entityId, request);
        } catch (Exception e) {
            logger.error("Unauthorized exception {}", e.getMessage());
            return createUnauthorizedExceptionResponse(e);
        }
        AttestationPolicy attestationPolicy = registryHelper.getAttestationPolicy(entityName, attestationName);
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEND, "OK", responseParams);
        if(attestationPolicy.isInternal()) {
            try {
                // Generate property Data
                String userId = registryHelper.getUserId(request, entityName);
                JsonNode entityNode = registryHelper.readEntity(userId, entityName, entityId, false, null, false)
                        .get(entityName);
                Map<String, List<String>> propertyOSIDMapper = objectMapper.convertValue(requestBody.get("propertiesOSID"), Map.class);
                JsonNode propertyData = JSONUtil.extractPropertyDataFromEntity(entityNode, attestationPolicy.getAttestationProperties(), propertyOSIDMapper);
                if(!propertyData.isNull()) {
                    ((ObjectNode)requestBody).put("propertyData", propertyData.toString());
                }
                registryHelper.addAttestationProperty(entityName, entityId, attestationName, requestBody, request);
                String attestationOSID = registryHelper.getAttestationOSID(requestBody, entityName, entityId, attestationName);
                // Resolve condition for REQUESTER
                String condition = conditionResolverService.resolve(propertyData, "REQUESTER", attestationPolicy.getConditions(), Collections.emptyList());
                updateGetFileUrl(additionalInput);
                // Rise claim
                PluginRequestMessage message = PluginRequestMessageCreator.create(
                        propertyData.toString(), condition, attestationOSID,
                        entityName,registryHelper.fetchEmailIdFromToken(request, entityName), entityId, additionalInput, Action.RAISE_CLAIM.name(), attestationPolicy.getName(),
                        attestationPolicy.getAttestorPlugin(), attestationPolicy.getAttestorEntity(),
                        attestationPolicy.getAttestorSignin());
                PluginRouter.route(message);
                response.setResult(Collections.singletonMap("attestationOSID", attestationOSID));
            } catch (Exception exception) {
                logger.error("Exception occurred while saving attestation data {}", exception.getMessage());
                exception.printStackTrace();
                responseParams.setErrmsg(exception.getMessage());
                response = new Response(Response.API_ID.SEND, HttpStatus.INTERNAL_SERVER_ERROR.toString(), responseParams);
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            try {
                registryHelper.addAttestationProperty(entityName, entityId, attestationName, requestBody, request);
                String attestationOSID = registryHelper.getAttestationOSID(requestBody, entityName, entityId, attestationName);
                PluginRequestMessage pluginRequestMessage = PluginRequestMessageCreator.create(
                        "", "", attestationOSID,
                        entityName, registryHelper.fetchEmailIdFromToken(request, entityName), entityId, additionalInput, Action.RAISE_CLAIM.name(), attestationPolicy.getName(),
                        attestationPolicy.getAttestorPlugin(), attestationPolicy.getAttestorEntity(),
                        attestationPolicy.getAttestorSignin());
                PluginRouter.route(pluginRequestMessage);
                response.setResult(Collections.singletonMap("attestationOSID", attestationOSID));
            } catch (Exception e) {
                logger.error("Unable to route to the actor : {}", e.getMessage());
                e.printStackTrace();
            }
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    private void updateGetFileUrl(JsonNode additionalInput) {
        if(additionalInput!= null && additionalInput.has("fileUrl")) {
            ArrayNode fileUrls = (ArrayNode)(additionalInput.get("fileUrl"));
            ArrayNode signedUrls = JsonNodeFactory.instance.arrayNode();
            for (JsonNode fileNode : fileUrls) {
                String fileUrl = fileNode.asText();
                try {
                    String sharableUrl = fileStorageService.getSignedUrl(fileUrl);
                    signedUrls.add(sharableUrl);
                } catch (ServerException | InternalException | XmlParserException | InvalidResponseException
                        | InvalidKeyException | NoSuchAlgorithmException | IOException
                        | ErrorResponseException | InsufficientDataException e) {
                    e.printStackTrace();
                }
            }
            ((ObjectNode)additionalInput).replace("fileUrl", signedUrls);
        }
    }
}
