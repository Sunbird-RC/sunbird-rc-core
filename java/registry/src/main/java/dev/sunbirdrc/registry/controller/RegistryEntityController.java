package dev.sunbirdrc.registry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.keycloak.OwnerCreationException;
import dev.sunbirdrc.pojos.AsyncRequest;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.Response;
import dev.sunbirdrc.pojos.ResponseParams;
import dev.sunbirdrc.registry.dao.NotFoundException;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.exception.AttestationNotFoundException;
import dev.sunbirdrc.registry.exception.RecordNotFoundException;
import dev.sunbirdrc.registry.exception.UnAuthorizedException;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.middleware.util.OSSystemFields;
import dev.sunbirdrc.registry.service.FileStorageService;
import dev.sunbirdrc.registry.service.ICertificateService;
import dev.sunbirdrc.registry.transform.Configuration;
import dev.sunbirdrc.registry.transform.Data;
import dev.sunbirdrc.registry.transform.ITransformer;
import dev.sunbirdrc.registry.util.ViewTemplateManager;
import dev.sunbirdrc.validators.ValidationException;
import org.agrona.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jetbrains.annotations.Nullable;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static dev.sunbirdrc.registry.Constants.*;
import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_TYPE;

@RestController
public class RegistryEntityController extends AbstractController {

    private static final String TRANSACTION_ID = "transactionId";
    private static Logger logger = LoggerFactory.getLogger(RegistryEntityController.class);

    @Autowired
    private ICertificateService certificateService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AsyncRequest asyncRequest;

    @Autowired
    private ViewTemplateManager viewTemplateManager;


    @Value("${authentication.enabled:true}") boolean securityEnabled;
    @Value("${certificate.enableExternalTemplates:false}") boolean externalTemplatesEnabled;

    @RequestMapping(value = "/api/v1/{entityName}/invite", method = RequestMethod.POST)
    public ResponseEntity<Object> invite(
            @PathVariable String entityName,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode rootNode,
            HttpServletRequest request
    ) throws Exception {
        final String TAG = "RegistryController:invite";
        logger.info("Inviting entity {}", rootNode);
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.INVITE, "OK", responseParams);
        Map<String, Object> result = new HashMap<>();
        ObjectNode newRootNode = objectMapper.createObjectNode();
        newRootNode.set(entityName, rootNode);
        try {
            registryHelper.authorizeInviteEntity(request, entityName);
            watch.start(TAG);
            String entityId = registryHelper.inviteEntity(newRootNode, "");
            registryHelper.autoRaiseClaim(entityName, entityId, "", null, newRootNode, dev.sunbirdrc.registry.Constants.USER_ANONYMOUS);
            Map resultMap = new HashMap();
            resultMap.put(dbConnectionInfoMgr.getUuidPropertyName(), entityId);
            result.put(entityName, resultMap);
            response.setResult(result);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.start(TAG);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (MiddlewareHaltException | ValidationException | OwnerCreationException e) {
            return badRequestException(responseParams, response, e.getMessage());
        }catch (UnAuthorizedException unAuthorizedException){
            return createUnauthorizedExceptionResponse(unAuthorizedException);
        }catch (Exception e) {
            if (e.getCause() != null && e.getCause().getCause() != null &&
                    e.getCause().getCause() instanceof InvocationTargetException) {
                Throwable targetException = ((InvocationTargetException) (e.getCause().getCause())).getTargetException();
                if (targetException instanceof OwnerCreationException) {
                    return badRequestException(responseParams, response, targetException.getMessage());
                }
            }
            return internalErrorResponse(responseParams, response, e);
        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.DELETE)
    public ResponseEntity<Object> deleteEntity(
      @PathVariable String entityName,
      @PathVariable String entityId,
      @RequestHeader HttpHeaders header,
      HttpServletRequest request
    ) {
        String userId = USER_ANONYMOUS;
        logger.info("Deleting entityType {} with Id {}", entityName, entityId);
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName)) {
            try {
                userId = registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                return createUnauthorizedExceptionResponse(e);
            }
        }
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.DELETE, "OK", responseParams);
        try {
            String tag = "RegistryController.delete " + entityName;
            watch.start(tag);
            Vertex deletedEntity = registryHelper.deleteEntity(entityId, userId);
            if (deletedEntity != null && deletedEntity.keys().contains(OSSystemFields._osSignedData.name())) {
                registryHelper.revokeExistingCredentials(entityName, entityId, userId, deletedEntity.value(OSSystemFields._osSignedData.name()));
            }
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop(tag);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("RegistryController: Exception while Deleting entity", e);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/search", method = RequestMethod.POST)
    public ResponseEntity<Object> searchEntity(@PathVariable String entityName, @RequestHeader HttpHeaders header, @RequestBody ObjectNode searchNode) {

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEARCH, "OK", responseParams);

        try {
            watch.start("RegistryController.searchEntity");
            ArrayNode entity = JsonNodeFactory.instance.arrayNode();
            entity.add(entityName);
            searchNode.set(ENTITY_TYPE, entity);
            if (definitionsManager.getDefinition(entityName).getOsSchemaConfiguration().getEnableSearch()) {
                JsonNode result = registryHelper.searchEntity(searchNode);
                watch.stop("RegistryController.searchEntity");
                return new ResponseEntity<>(result.get(entityName), HttpStatus.OK);
            } else {
                watch.stop("RegistryController.searchEntity");
                logger.error("Searching on entity {} not allowed", entityName);
                response.setResult("");
                responseParams.setStatus(Response.Status.UNSUCCESSFUL);
                responseParams.setErrmsg(String.format("Searching on entity %s not allowed", entityName));
            }
        } catch (Exception e) {
            logger.error("Exception in controller while searching entities !", e);
            response.setResult("");
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.PUT)
    public ResponseEntity<Object> putEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestBody JsonNode rootNode,
            HttpServletRequest request) {

        logger.info("Updating entityType {} request body {}", entityName, rootNode);
        String userId = USER_ANONYMOUS;
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName)) {
            try {
                userId = registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                return createUnauthorizedExceptionResponse(e);
            }
        }
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        ((ObjectNode) rootNode).put(uuidPropertyName, entityId);
        ObjectNode newRootNode = objectMapper.createObjectNode();
        newRootNode.set(entityName, rootNode);

        try {
            String tag = "RegistryController.update " + entityName;
            watch.start(tag);
            // TODO: get userID from auth header
            JsonNode existingNode = registryHelper.readEntity(newRootNode, userId);
            String emailId = registryHelper.fetchEmailIdFromToken(request, entityName);
            registryHelper.updateEntityAndState(existingNode, newRootNode, userId);
            if (existingNode.get(entityName).has(OSSystemFields._osSignedData.name())) {
                registryHelper.revokeExistingCredentials(entityName, entityId, userId,
                        existingNode.get(entityName).get(OSSystemFields._osSignedData.name()).asText(""));
            }
            registryHelper.invalidateAttestation(entityName, entityId, userId,null);
            registryHelper.autoRaiseClaim(entityName, entityId, userId, existingNode, newRootNode, emailId);
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop(tag);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("RegistryController: Exception while updating entity (without id)!", e);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @RequestMapping(value = "/api/v1/{entityName}", method = RequestMethod.POST)
    public ResponseEntity<Object> postEntity(
            @PathVariable String entityName,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode rootNode,
            @RequestParam(defaultValue = "sync") String mode,
            @RequestParam(defaultValue = "${webhook.url}") String callbackUrl,
            HttpServletRequest request
    ) {
        logger.info("MODE: {}", asyncRequest.isEnabled());
        logger.info("MODE: {}", asyncRequest.getWebhookUrl());
        logger.info("Adding entity {}", rootNode);
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.CREATE, "OK", responseParams);
        Map<String, Object> result = new HashMap<>();
        ObjectNode newRootNode = objectMapper.createObjectNode();
        newRootNode.set(entityName, rootNode);

        try {
            String userId = registryHelper.authorizeManageEntity(request, entityName);
            String label = registryHelper.addEntity(newRootNode, userId);
            String emailId = registryHelper.fetchEmailIdFromToken(request, entityName);
            Map<String, String> resultMap = new HashMap<>();
            if (asyncRequest.isEnabled()) {
                resultMap.put(TRANSACTION_ID, label);
            } else {
                registryHelper.autoRaiseClaim(entityName, label, userId, null, newRootNode, emailId);
                resultMap.put(dbConnectionInfoMgr.getUuidPropertyName(), label);
            }
            result.put(entityName, resultMap);
            response.setResult(result);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop("RegistryController.addToExistingEntity");

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (MiddlewareHaltException e) {
            logger.info("Error in validating the request");
            return badRequestException(responseParams, response, e.getMessage());
        } catch (Exception e) {
            logger.error("Exception in controller while adding entity !", e);
            response.setResult(result);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @RequestMapping(value = "/api/v1/{entityName}/{entityId}/**", method = RequestMethod.PUT)
    public ResponseEntity<Object> updatePropertyOfTheEntity(
            HttpServletRequest request,
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestBody JsonNode requestBody

    ) {
        String userId = USER_ANONYMOUS;
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName)) {
            try {
                userId = registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                return createUnauthorizedExceptionResponse(e);
            }
        }
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);

        try {
            String tag = "RegistryController.update " + entityName;
            watch.start(tag);
            requestBody = registryHelper.removeFormatAttr(requestBody);
            JsonNode existingNode = registryHelper.readEntity(userId, entityName, entityId, false, null, false);
            registryHelper.updateEntityProperty(entityName, entityId, requestBody, request, existingNode);
            if (existingNode.get(entityName).has(OSSystemFields._osSignedData.name())) {
                registryHelper.revokeExistingCredentials(entityName, entityId, userId,
                        existingNode.get(entityName).get(OSSystemFields._osSignedData.name()).asText(""));
            }
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            registryHelper.invalidateAttestation(entityName, entityId, userId,registryHelper.getPropertyToUpdate(request,entityId));
            watch.stop(tag);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}/**", method = RequestMethod.POST)
    public ResponseEntity<Object> addNewPropertyToTheEntity(
            HttpServletRequest request,
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode requestBody
    ) {
        try {
            registryHelper.authorize(entityName, entityId, request);
        } catch (Exception e) {
            return createUnauthorizedExceptionResponse(e);
        }
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        try {
            String tag = "RegistryController.addNewPropertyToTheEntity " + entityName;
            watch.start(tag);
            String notes = getNotes(requestBody);
            requestBody = registryHelper.removeFormatAttr(requestBody);
            registryHelper.addEntityProperty(entityName, entityId, requestBody, request);
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop(tag);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    private String getNotes(JsonNode requestBody) {
        String notes = "";
        if (requestBody.has("notes")) {
            notes = requestBody.get("notes").asText();
            JSONUtil.removeNodes(requestBody, Collections.singletonList("notes"));
        }
        return notes;
    }

    private JsonNode getAttestationSignedData(String attestationId, JsonNode node) throws AttestationNotFoundException, JsonProcessingException {
        JsonNode attestationNode = getAttestationNode(attestationId, node);
        if(attestationNode.get(OSSystemFields._osAttestedData.name()) == null) throw new AttestationNotFoundException();
        attestationNode = objectMapper.readTree(attestationNode.get(OSSystemFields._osAttestedData.name()).asText());
        return attestationNode;
    }

    @Nullable
    private JsonNode getAttestationNode(String attestationId, JsonNode node) {
        Iterator<JsonNode> iterator = node.iterator();
        JsonNode attestationNode = null;
        while(iterator.hasNext()) {
            attestationNode = iterator.next();
            if (attestationNode.get(uuidPropertyName).toString().equals(attestationId)) {
                break;
            }
        }
        return attestationNode;
    }

    @RequestMapping(value = "/partner/api/v1/{entityName}", method = RequestMethod.GET)
    public ResponseEntity<Object> getEntityWithConsent(
            @PathVariable String entityName,
            HttpServletRequest request) {
        try {
            ArrayList<String> fields = getConsentFields(request);
            JsonNode userInfoFromRegistry = registryHelper.getRequestedUserDetails(request, entityName);
            JsonNode jsonNode = userInfoFromRegistry.get(entityName);
            if (jsonNode instanceof ArrayNode) {
                ArrayNode values = (ArrayNode) jsonNode;
                if (values.size() > 0) {
                    JsonNode node = values.get(0);
                    if (node instanceof ObjectNode) {
                        ObjectNode entityNode = copyWhiteListedFields(fields, node);
                        return new ResponseEntity<>(entityNode, HttpStatus.OK);
                    }
                }
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RecordNotFoundException ex) {
            logger.error("Error in finding the entity", ex);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error in partner api access", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ObjectNode copyWhiteListedFields(ArrayList<String> fields, JsonNode dataNode) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        for (String key : fields) {
            node.set(key, dataNode.get(key));
        }
        return node;
    }

    private ArrayList<String> getConsentFields(HttpServletRequest request) {
        ArrayList<String> fields = new ArrayList<>();
        KeycloakAuthenticationToken principal = (KeycloakAuthenticationToken) request.getUserPrincipal();
        try {
            Map<String, Object> otherClaims = ((KeycloakPrincipal) principal.getPrincipal()).getKeycloakSecurityContext().getToken().getOtherClaims();
            if (otherClaims.keySet().contains(dev.sunbirdrc.registry.Constants.KEY_CONSENT) && otherClaims.get(dev.sunbirdrc.registry.Constants.KEY_CONSENT) instanceof Map) {
                Map consentFields = (Map) otherClaims.get(dev.sunbirdrc.registry.Constants.KEY_CONSENT);
                for (Object key : consentFields.keySet()) {
                    fields.add(key.toString());
                }
            }
        } catch (Exception ex) {
            logger.error("Error while extracting other claims", ex);
        }
        return fields;
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.GET, produces =
            {MediaType.APPLICATION_PDF_VALUE, MediaType.TEXT_HTML_VALUE, Constants.SVG_MEDIA_TYPE})
    public ResponseEntity<Object> getEntityType(@PathVariable String entityName,
                                                @PathVariable String entityId,
                                                HttpServletRequest request,
                                                @RequestHeader(required = false) String viewTemplateId) {
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName) && securityEnabled) {
            try {
                registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                try {
                    registryHelper.authorizeAttestor(entityName, request);
                } catch (Exception exceptionFromAuthorizeAttestor) {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
            }
        }
        try {
            String readerUserId = getUserId(entityName, request);
            JsonNode node = registryHelper.readEntity(readerUserId, entityName, entityId, false,
                            viewTemplateManager.getViewTemplateById(viewTemplateId), false)
                    .get(entityName);
            JsonNode signedNode = objectMapper.readTree(node.get(OSSystemFields._osSignedData.name()).asText());
            return new ResponseEntity<>(certificateService.getCertificate(signedNode,
                    entityName,
                    entityId,
                    request.getHeader(HttpHeaders.ACCEPT),
                    getTemplateUrlFromRequest(request, entityName),
                    JSONUtil.removeNodesByPath(node, definitionsManager.getExcludingFieldsForEntity(entityName))
            ), HttpStatus.OK);
        } catch (Exception exception) {
            exception.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private String getTemplateUrlFromRequest(HttpServletRequest request, String entityName) {
        if (externalTemplatesEnabled && !StringUtils.isEmpty(request.getHeader(Template))) {
            return request.getHeader(Template);
        }
        if (definitionsManager.getCertificateTemplates(entityName).size() > 0 && !StringUtils.isEmpty(request.getHeader(TemplateKey))) {
            String templateUri = definitionsManager.getCertificateTemplates(entityName).getOrDefault(request.getHeader(TemplateKey), null);
            if (!StringUtils.isEmpty(templateUri)) {
                try {
                    if (templateUri.startsWith(MINIO_URI_PREFIX)) {
                        return fileStorageService.getSignedUrl(templateUri.substring(MINIO_URI_PREFIX.length()));
                    } else if (templateUri.startsWith(HTTP_URI_PREFIX) || templateUri.startsWith(HTTPS_URI_PREFIX)) {
                        return templateUri;
                    }
                } catch (Exception e) {
                    logger.error("Exception while parsing certificate templates DID urls", e);
                    return null;
                }
            }

        }
        return null;
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.GET)
    public ResponseEntity<Object> getEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestHeader HttpHeaders header, HttpServletRequest request,
            @RequestHeader(required = false) String viewTemplateId) {
        boolean requireLDResponse = false;
        boolean requireVCResponse = false;
        for (MediaType t: header.getAccept()) {
            if (t.toString().equals(Constants.LD_JSON_MEDIA_TYPE)) {
                requireLDResponse = true;
                break;
            } else if (t.toString().equals(Constants.VC_JSON_MEDIA_TYPE)) {
                requireVCResponse = true;
            }
        }
        if (registryHelper.doesEntityOperationRequireAuthorization(entityName) && securityEnabled) {
            try {
                registryHelper.authorize(entityName, entityId, request);
            } catch (Exception e) {
                try {
                    registryHelper.authorizeAttestor(entityName, request);
                } catch (Exception exceptionFromAuthorizeAttestor) {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
            }
        }
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.READ, "OK", responseParams);
        try {
            String readerUserId = getUserId(entityName, request);
            JsonNode node = getEntityJsonNode(entityName, entityId, requireLDResponse, readerUserId, viewTemplateId);
            if(requireLDResponse) {
                addJsonLDSpec(node);
            } else if (requireVCResponse) {
                String vcString = node.get(OSSystemFields._osSignedData.name()).textValue();
                return new ResponseEntity<>(vcString, HttpStatus.OK);
            }
            return new ResponseEntity<>(node, HttpStatus.OK);

        } catch (NotFoundException | RecordNotFoundException e) {
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Read Api Exception occurred ", e);
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getUserId(String entityName, HttpServletRequest request) throws Exception {
        return registryHelper.getUserId(request, entityName);
    }

    private void addJsonLDSpec(JsonNode node) {

    }

    private JsonNode getEntityJsonNode(@PathVariable String entityName, @PathVariable String entityId,
                                       boolean requireLDResponse, String userId, String viewTemplateId) throws Exception {
        JsonNode resultNode = registryHelper.readEntity(userId, entityName, entityId, false,
                viewTemplateManager.getViewTemplateById(viewTemplateId), false);
        // Transformation based on the mediaType
        Data<Object> data = new Data<>(resultNode);
        Configuration config = configurationHelper.getResponseConfiguration(requireLDResponse);

        ITransformer<Object> responseTransformer = transformer.getInstance(config);
        Data<Object> resultContent = responseTransformer.transform(data);
        logger.info("ReadEntity,{},{}", entityId, resultContent);
        if (!(resultContent.getData() instanceof JsonNode)) {
            throw new RuntimeException("Unknown response object " + resultContent);
        }
        JsonNode node = (JsonNode) resultContent.getData();
        JsonNode entityNode = node.get(entityName);
        return entityNode!=null?entityNode:node;
    }

    @RequestMapping(value = "/api/v1/{entityName}", method = RequestMethod.GET)
    public ResponseEntity<Object> getEntityByToken(@PathVariable String entityName, HttpServletRequest request,
                                                   @RequestHeader(required = false) String viewTemplateId) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEARCH, "OK", responseParams);
        try {
            String userId = registryHelper.getUserId(request, entityName);
            if (!Strings.isEmpty(userId)) {
                JsonNode responseFromDb = registryHelper.searchEntitiesByUserId(entityName, userId, viewTemplateId);
                return new ResponseEntity<>(responseFromDb.get(entityName), HttpStatus.OK);
            } else {
                responseParams.setErrmsg("User id is empty");
                responseParams.setStatus(Response.Status.UNSUCCESSFUL);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (RecordNotFoundException e) {
            logger.error("Exception in controller while searching entities !", e);
            response.setResult("");
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Exception in controller while searching entities !", e);
            response.setResult("");
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }

    //TODO: check the usage and deprecate the api if not used
    @GetMapping(value = "/api/v1/{entity}/{entityId}/attestationProperties")
    public ResponseEntity<Object> getEntityForAttestation(
            @PathVariable String entity,
            @PathVariable String entityId
    ) {
        try {
            JsonNode resultNode = registryHelper.readEntity("", entity, entityId, false, null, false);
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.set("entity", resultNode.get(entity));
            List<AttestationPolicy> attestationPolicies = definitionsManager.getDefinition(entity)
                    .getOsSchemaConfiguration()
                    .getAttestationPolicies();
            objectNode.set("attestationPolicies", objectMapper.convertValue(attestationPolicies, JsonNode.class));
            return new ResponseEntity<>(objectNode, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
    //TODO: check the usage and deprecate the api if not used
    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.PATCH)
    public ResponseEntity<Object> attestEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode rootNode
    ) throws Exception {
        logger.info("Attestation request for {}", rootNode.get("fieldPaths"));
        JsonNode nodePath = rootNode.get("jsonPaths");
        if (nodePath instanceof ArrayNode) {
            Iterator<JsonNode> elements = ((ArrayNode) nodePath).elements();
            ArrayList<String> paths = new ArrayList<>();
            for (Iterator<JsonNode> it = elements; it.hasNext(); ) {
                JsonNode e = it.next();
                paths.add(e.textValue());
            }
            JsonNode node = registryHelper.readEntity("admin", entityName, entityId, false, null, false);
            registryHelper.attestEntity(entityName, node, paths.toArray(new String[]{}), "admin");
        }
        return null;
    }

    //TODO: check the usage and deprecate the api if not used
    @RequestMapping(value = "/api/v1/system/{property}/{propertyId}", method = RequestMethod.POST)
    public ResponseEntity<ResponseParams> updateProperty(
            @PathVariable String property,
            @PathVariable String propertyId,
            @RequestBody JsonNode requestBody) {
        logger.info("Got system request for the property {} {}", property, propertyId);
        ((ObjectNode) requestBody).put(uuidPropertyName, propertyId);
        ObjectNode newRootNode = objectMapper.createObjectNode();

        ResponseParams responseParams = new ResponseParams();
        newRootNode.set(property, requestBody);
        try {
            String response = registryHelper.updateProperty(newRootNode, "");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            responseParams.setResultList(Collections.singletonList(response));
            return new ResponseEntity<>(responseParams, HttpStatus.OK);
        } catch (Exception exception) {
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(exception.getMessage());
            exception.printStackTrace();
            return new ResponseEntity<>(responseParams, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //TODO: API called by claim-ms, need to be blocked from external access
    @RequestMapping(value = "/api/v1/{property}/{propertyId}/attestation/{attestationName}/{attestationId}", method = RequestMethod.PUT)
    public ResponseEntity<ResponseParams> updateAttestationProperty(
            @PathVariable String property,
            @PathVariable String propertyId,
            @PathVariable String attestationName,
            @PathVariable String attestationId,
            @RequestBody JsonNode requestBody) {
        logger.info("Got system request to update attestation property {} {} {} {}", property, propertyId, attestationName, attestationId);
        ((ObjectNode) requestBody).put(uuidPropertyName, propertyId);
        ObjectNode newRootNode = objectMapper.createObjectNode();

        ResponseParams responseParams = new ResponseParams();
        newRootNode.set(property, requestBody);
        try {
            logger.info("updateAttestationProperty: {}", requestBody);
            PluginResponseMessage pluginResponseMessage = objectMapper.convertValue(requestBody, PluginResponseMessage.class);
            registryHelper.updateState(pluginResponseMessage);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            responseParams.setResultList(Collections.singletonList("response"));
            return new ResponseEntity<>(responseParams, HttpStatus.OK);
        } catch (Exception exception) {
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(exception.getMessage());
            exception.printStackTrace();
            return new ResponseEntity<>(responseParams, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Deprecated
    @RequestMapping(value = "/api/v1/{entityName}/sign", method = RequestMethod.GET)
    public ResponseEntity<Object> getSignedEntityByToken(@PathVariable String entityName, HttpServletRequest request) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEARCH, "OK", responseParams);
        try {
            JsonNode result = registryHelper.getRequestedUserDetails(request, entityName);
            if (result.get(entityName).size() > 0) {
                Object credentialTemplate = definitionsManager.getCredentialTemplate(entityName);
                Object signedCredentials = registryHelper.getSignedDoc(result.get(entityName).get(0), credentialTemplate);
                return new ResponseEntity<>(signedCredentials, HttpStatus.OK);
            } else {
                responseParams.setErrmsg("Entity not found");
                responseParams.setStatus(Response.Status.UNSUCCESSFUL);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Exception in controller while searching entities !", e);
            response.setResult("");
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/api/v1/{entityName}/{entityId}/attestation/{attestationName}/{attestationId}",
            produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.TEXT_HTML_VALUE, Constants.SVG_MEDIA_TYPE})
    public ResponseEntity<Object> getAttestationCertificate(HttpServletRequest request, @PathVariable String entityName, @PathVariable String entityId,
                                                            @PathVariable String attestationName, @PathVariable String attestationId) {
        try {
            String readerUserId = getUserId(entityName, request);
            JsonNode node = registryHelper.readEntity(readerUserId, entityName, entityId, false, null, false)
                    .get(entityName).get(attestationName);
            JsonNode attestationNode = getAttestationSignedData(attestationId, node);
            return new ResponseEntity<>(certificateService.getCertificate(attestationNode,
                    entityName,
                    entityId,
                    request.getHeader(HttpHeaders.ACCEPT),
                    getTemplateUrlFromRequest(request, entityName),
                    getAttestationNode(attestationId, node)
            ), HttpStatus.OK);
        } catch (AttestationNotFoundException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
