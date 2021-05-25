package io.opensaber.registry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.dao.NotFoundException;
import io.opensaber.registry.helper.RegistryHelper;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.pojos.attestation.AttestationPolicy;
import io.opensaber.registry.model.state.StateContext;
import io.opensaber.registry.service.*;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.registry.transform.Configuration;
import io.opensaber.registry.transform.ConfigurationHelper;
import io.opensaber.registry.transform.Data;
import io.opensaber.registry.transform.ITransformer;
import io.opensaber.registry.transform.Transformer;
import io.opensaber.registry.util.*;

import io.opensaber.validators.IValidate;
import io.swagger.models.Operation;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.*;

@RestController
public class RegistryController {
    private static Logger logger = LoggerFactory.getLogger(RegistryController.class);
    @Autowired
    Transformer transformer;
    @Autowired
    private ConfigurationHelper configurationHelper;
    @Autowired
    private RegistryService registryService;
    @Autowired
    private ISearchService searchService;
    @Autowired
    private IReadService readService;
    @Autowired
    private APIMessage apiMessage;
    @Autowired
    private DBConnectionInfoMgr dbConnectionInfoMgr;
    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;
    @Autowired
    private OpenSaberInstrumentation watch;
    @Autowired
    private KeycloakAdminUtil keycloakAdminUtil;
    @Autowired
    private ShardManager shardManager;
    @Autowired
    ClaimRequestClient claimRequestClient;

    @Autowired
    private ViewTemplateManager viewTemplateManager;
    @Autowired
    private NativeReadService nativeReadService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegistryHelper registryHelper;

    @Value("${audit.enabled}")
    private boolean auditEnabled;

    @Value("${audit.frame.store}")
    public String auditStoreType;

    @Autowired
    private DefinitionsManager definitionsManager;

    @Autowired
    private IValidate validationService;

    @Autowired
    RuleEngineService ruleEngineService;

    /**
     * Note: Only one mime type is supported at a time. Pick up the first mime
     * type from the header.
     *
     * @return
     */
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public ResponseEntity<Response> searchEntity(@RequestHeader HttpHeaders header) {

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.SEARCH, "OK", responseParams);
        JsonNode payload = apiMessage.getRequest().getRequestMapNode();

        try {
            watch.start("RegistryController.searchEntity");
            JsonNode result = registryHelper.searchEntity(payload);

            response.setResult(result);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop("RegistryController.searchEntity");
        } catch (Exception e) {
            logger.error("Exception in controller while searching entities !", e);
            response.setResult("");
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public ResponseEntity<Response> health() {

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.HEALTH, "OK", responseParams);

        try {
            Shard shard = shardManager.getDefaultShard();
            HealthCheckResponse healthCheckResult = registryService.health(shard);
            response.setResult(JSONUtil.convertObjectJsonMap(healthCheckResult));
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            logger.debug("Application heath checked : ", healthCheckResult.toString());
        } catch (Exception e) {
            logger.error("Error in health checking!", e);
            HealthCheckResponse healthCheckResult = new HealthCheckResponse(Constants.OPENSABER_REGISTRY_API_NAME,
                    false, null);
            response.setResult(JSONUtil.convertObjectJsonMap(healthCheckResult));
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg("Error during health check");
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ResponseBody
	@RequestMapping(value = "/audit", method = RequestMethod.POST)
	public ResponseEntity<Response> fetchAudit() {
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.AUDIT, "OK", responseParams);
		JsonNode payload = apiMessage.getRequest().getRequestMapNode();
		if (auditEnabled && Constants.DATABASE.equals(auditStoreType)) {
			try {
				watch.start("RegistryController.audit");
				JsonNode result = registryHelper.getAuditLog(payload);

				response.setResult(result);
				responseParams.setStatus(Response.Status.SUCCESSFUL);
				watch.stop("RegistryController.searchEntity");

			} catch (Exception e) {
				logger.error("Error in getting audit log !", e);
				logger.error("Exception in controller while searching entities !", e);
				response.setResult("");
				responseParams.setStatus(Response.Status.UNSUCCESSFUL);
				responseParams.setErrmsg(e.getMessage());
			}
		}else {
			response.setResult("");
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Audit is not enabled or file is chosen to store the audit");
			return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResponseEntity<Response> deleteEntity() {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.DELETE, "OK", responseParams);
        try {
            String entityType = apiMessage.getRequest().getEntityType();
            String entityId = apiMessage.getRequest().getRequestMapNode().get(entityType).get(dbConnectionInfoMgr.getUuidPropertyName()).asText();
            RecordIdentifier recordId = RecordIdentifier.parse(entityId);
            String shardId = dbConnectionInfoMgr.getShardId(recordId.getShardLabel());
            Shard shard = shardManager.activateShard(shardId);
            registryService.deleteEntityById(shard,apiMessage.getUserID(),recordId.getUuid());
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
        } catch (UnsupportedOperationException e) {
            logger.error("Controller: UnsupportedOperationException while deleting entity !", e);
            response.setResult(null);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
        } catch (Exception e) {
            logger.error("Controller: Exception while deleting entity !", e);
            response.setResult(null);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg("Meh ! You encountered an error!");
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResponseEntity<Response> addEntity() {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.CREATE, "OK", responseParams);
        Map<String, Object> result = new HashMap<>();
        String entityType = apiMessage.getRequest().getEntityType();
        JsonNode rootNode = apiMessage.getRequest().getRequestMapNode();

        try {
            String label = registryHelper.addEntity(rootNode,apiMessage.getUserID());
            Map resultMap = new HashMap();
            resultMap.put(dbConnectionInfoMgr.getUuidPropertyName(), label);

            result.put(entityType, resultMap);
            response.setResult(result);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop("RegistryController.addToExistingEntity");
        } catch (Exception e) {
            logger.error("Exception in controller while adding entity !", e);
            response.setResult(result);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Reads the entity. If there is application/ld+json used in the header,
     * then read will respect this. Defaults to application/json otherwise.
     *
     * @param header
     * @return
     */
    @RequestMapping(value = "/read", method = RequestMethod.POST)
    public ResponseEntity<Response> readEntity(@RequestHeader HttpHeaders header, Principal principal) {
        boolean requireLDResponse = header.getAccept().contains(Constants.LD_JSON_MEDIA_TYPE);

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.READ, "OK", responseParams);
        JsonNode inputJson = apiMessage.getRequest().getRequestMapNode();
        try {
            JsonNode resultNode = registryHelper.readEntity(inputJson, principal.getName(), requireLDResponse);
            // Transformation based on the mediaType
            Data<Object> data = new Data<>(resultNode);
            Configuration config = configurationHelper.getResponseConfiguration(requireLDResponse);

            ITransformer<Object> responseTransformer = transformer.getInstance(config);
            Data<Object> resultContent = responseTransformer.transform(data);
            response.setResult(resultContent.getData());
            logger.info("ReadEntity,{},{}", resultNode.get(apiMessage.getRequest().getEntityType()).get(uuidPropertyName), config);
        } catch (Exception e) {
            logger.error("Read Api Exception occurred ", e);
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @RequestMapping(value = "/attest", method = RequestMethod.GET)
    public ResponseEntity<Response> attest(@RequestHeader HttpHeaders header) {
        /*
         * check for the attester role.
         * mark as attested.
         * save the entity.
         */
        return null;
    }
    @RequestMapping(value = "/invite", method = RequestMethod.POST)
    public ResponseEntity<Response> invite(@RequestHeader HttpHeaders header) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.INVITE, "OK", responseParams);
        Map<String, Object> result = new HashMap<>();
        String entityType = apiMessage.getRequest().getEntityType();
        JsonNode rootNode = apiMessage.getRequest().getRequestMapNode();

        try {
            String entitySubject = validationService.getEntitySubject(entityType, rootNode);
            String userID = keycloakAdminUtil.createUser(entitySubject, "facility admin");
            logger.info("Owner user_id : " + userID);
            String label = registryHelper.inviteEntity(rootNode, apiMessage.getUserID(), userID);
            Map resultMap = new HashMap();
            resultMap.put(dbConnectionInfoMgr.getUuidPropertyName(), label);
            result.put(entityType, resultMap);
            response.setResult(result);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
        } catch (Exception e) {
            logger.error("Exception in controller while adding entity !", e);
            response.setResult(result);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @RequestMapping(value = "/registers", method = RequestMethod.GET)
    public ResponseEntity<Response> getRegisters(@RequestHeader HttpHeaders header) {
        boolean requireLDResponse = header.getAccept().contains(Constants.LD_JSON_MEDIA_TYPE);

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.READ, "OK", responseParams);
        try {
            Set<String> registryList = definitionsManager.getAllKnownDefinitions();
            response.setResult(registryList);
            logger.info("get registers,{}", registryList);
        } catch (Exception e) {
            logger.error("Read Api Exception occurred ", e);
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public ResponseEntity<Response> updateEntity() {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);

        JsonNode inputJson = apiMessage.getRequest().getRequestMapNode();
        try {
            watch.start("RegistryController.update");
            registryHelper.updateEntity(inputJson, apiMessage.getUserID());
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop("RegistryController.update");
        } catch (Exception e) {
            logger.error("RegistryController: Exception while updating entity (without id)!", e);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.PUT)
    public ResponseEntity<Object> putEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode rootNode
    ) throws JsonProcessingException {
        logger.info("Updating entityType {} request body {}", entityName, rootNode);
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        if (rootNode == null) {
            logger.error("Bad request body {}", rootNode);
            return badRequestException(responseParams, response, "Request body is empty");
        }
        if (rootNode.has(uuidPropertyName)) {
            if (!rootNode.get(uuidPropertyName).asText().equals(entityId)) {
                logger.error("Bad request body {}", rootNode);
                return badRequestException(responseParams, response, "ID passed in params doesn't match with the Request body ID");
            }
        } else {
            ((ObjectNode)rootNode).put(uuidPropertyName, entityId);
        }
        ObjectNode newRootNode = objectMapper.createObjectNode();
        newRootNode.set(entityName, rootNode);

        try {
            String tag = "RegistryController.update " + entityName;
            watch.start(tag);
            // TODO: get userID from auth header
            registryHelper.updateEntity(newRootNode, "");
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
            @RequestBody JsonNode rootNode
    ) throws JsonProcessingException {
        logger.info("Adding entity {}", rootNode);
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.CREATE, "OK", responseParams);
        Map<String, Object> result = new HashMap<>();
        if (rootNode == null) {
            logger.info("Bad request body {}", rootNode);
            return badRequestException(responseParams, response, "Request body is empty");
        }
        ObjectNode newRootNode = objectMapper.createObjectNode();
        newRootNode.set(entityName, rootNode);

        try {
            validationService.validate(entityName, objectMapper.writeValueAsString(newRootNode));
        } catch (MiddlewareHaltException e) {
            logger.info("Error in validating the request");
            return badRequestException(responseParams, response, e.getMessage());
        }

        try {
            String label = registryHelper.addEntity(newRootNode, ""); //todo add user id from auth scope.
            Map resultMap = new HashMap();
            resultMap.put(dbConnectionInfoMgr.getUuidPropertyName(), label);

            result.put(entityName, resultMap);
            response.setResult(result);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop("RegistryController.addToExistingEntity");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Exception in controller while adding entity !", e);
            response.setResult(result);
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            responseParams.setErrmsg(e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<Object> badRequestException(ResponseParams responseParams, Response response, String errorMessage) {
        responseParams.setStatus(Response.Status.UNSUCCESSFUL);
        responseParams.setErrmsg(errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Entity name and entityId will be used for ESActor
    @RequestMapping(value="/api/v1/{entityName}/{entityId}/{property}/attest")
    public ResponseEntity<Object> attest(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @PathVariable String property,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode requestBody
    ) throws IOException {
        String id = requestBody.get(uuidPropertyName).asText();
        try {
            JsonNode node = registryHelper.readEntity("admin", property, id, false, null, false);
            Map<String, Object> result = JSONUtil.convertJsonNodeToMap(node.get(property));
            // Do the state transitions based on valid and invalid fields
            logger.info("Received ", result.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}/{property}/{propertyId}", method = RequestMethod.PUT)
    public ResponseEntity<Object> addNewPropertyToTheEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @PathVariable String property,
            @PathVariable String propertyId,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode requestBody
    ) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        try {

            String userId = "";
            JsonNode existingNode = registryHelper
                    .readEntity(userId, property, propertyId, false, null, false)
                    .get(property);
            StateContext stateContext = new StateContext(existingNode, requestBody, "student");
            ruleEngineService.doTransition(stateContext);
            ObjectNode newRootNode = objectMapper.createObjectNode();
            newRootNode.set(property, stateContext.getResult());
            String tag = "RegistryController.update " + entityName;
            watch.start(tag);
            registryHelper.updateEntity(newRootNode, userId);
            registryHelper.updateEntityInEs(entityName, entityId);
            if(requestBody.has("send") && requestBody.get("send").asBoolean()) {
                claimRequestClient.riseClaimRequest(entityName, entityId, property, propertyId);
            }
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

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}/{property}/{propertyId}/send", method = RequestMethod.POST)
    public ResponseEntity<Object> sendForVerification(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @PathVariable String property,
            @PathVariable String propertyId,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode requestBody
    ) {
        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        try {
            String userId = "";
            String currentRole = "student";
            JsonNode existingNode = registryHelper
                    .readEntity(userId, property, propertyId, false, null, false)
                    .get(property);
            StateContext stateContext = new StateContext(existingNode, currentRole);
            ruleEngineService.doTransition(stateContext);
            ObjectNode newRootNode = objectMapper.createObjectNode();
            newRootNode.set(property, stateContext.getResult());
            String tag = "RegistryController.update " + entityName;
            watch.start(tag);
            // update the state
            registryHelper.updateEntity(newRootNode, userId);
            registryHelper.updateEntityInEs(entityName, entityId);
            claimRequestClient.riseClaimRequest(entityName, entityId, property, propertyId);
            responseParams.setErrmsg(userId);
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            watch.stop(tag);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}/{property}", method = RequestMethod.POST)
    public ResponseEntity<Object> addNewPropertyToTheEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @PathVariable String property,
            @RequestHeader HttpHeaders header,
            @RequestBody JsonNode requestBody
    ) {
        // TODO: Add Auth validation & property validation
        // TODO: get userID from auth header

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);
        try {
            ObjectNode propertyNode = objectMapper.createObjectNode();
            StateContext state = new StateContext("student", requestBody);
            ruleEngineService.doTransition(state);
            JsonNode existingProperties = registryHelper
                    .readEntity("", entityName, entityId, false, null, false)
                    .get(entityName)
                    .get(property);
            if(existingProperties != null) {
                propertyNode.set(property, ((ArrayNode)existingProperties).add(state.getResult()));
                propertyNode.put(uuidPropertyName, entityId);
            } else {
                propertyNode.set(property, JsonNodeFactory.instance.arrayNode().add(state.getResult()));
                propertyNode.put(uuidPropertyName, entityId);
            }

            ObjectNode newRootNode = objectMapper.createObjectNode();
            newRootNode.set(entityName, propertyNode);
            String tag = "RegistryController.update " + entityName;
            watch.start(tag);
            registryHelper.updateEntity(newRootNode, "");
            responseParams.setErrmsg("");
            responseParams.setStatus(Response.Status.SUCCESSFUL);
            registryHelper.updateEntityInEs(entityName, entityId);
            watch.stop(tag);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            responseParams.setErrmsg(e.getMessage());
            responseParams.setStatus(Response.Status.UNSUCCESSFUL);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/api/v1/{entityName}/{entityId}", method = RequestMethod.GET)
    public ResponseEntity<Object> getEntity(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestHeader HttpHeaders header) {
        boolean requireLDResponse = header.getAccept().contains(Constants.LD_JSON_MEDIA_TYPE);

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.READ, "OK", responseParams);
        try {
            JsonNode resultNode = registryHelper.readEntity("", entityName, entityId, false, null, false);
            // Transformation based on the mediaType
            Data<Object> data = new Data<>(resultNode);
            Configuration config = configurationHelper.getResponseConfiguration(requireLDResponse);

            ITransformer<Object> responseTransformer = transformer.getInstance(config);
            Data<Object> resultContent = responseTransformer.transform(data);
            logger.info("ReadEntity,{},{}", entityId, resultContent);
            if (resultContent.getData() instanceof JsonNode) {
                JsonNode node = (JsonNode) resultContent.getData();
                return new ResponseEntity<>(node.get(entityName), HttpStatus.OK);
            } else {
                logger.error("Unknown response object {}", resultContent);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (NotFoundException e) {
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

    @GetMapping(value="/api/v1/{entity}/{entityId}/attestationProperties")
    public ResponseEntity<Object> getEntityForAttestation(
            @PathVariable String entity,
            @PathVariable String entityId
    ) {
        try {
            JsonNode resultNode = registryHelper.readEntity("", entity, entityId, false, null, false);
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.set(entity, resultNode.get(entity));
            List<AttestationPolicy> attestationPolicies = definitionsManager.getDefinition(entity)
                    .getOsSchemaConfiguration()
                    .getAttestationPolicies();
            objectNode.set("policy", objectMapper.convertValue(attestationPolicies, JsonNode.class));
            return new ResponseEntity<>(resultNode, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @RequestMapping(value = "/api/docs/swagger.json", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Object> getSwaggerDoc() throws IOException {
        ObjectNode doc = (ObjectNode) objectMapper.reader().readTree(new ClassPathResource("/baseSwagger.json").getInputStream());
        ObjectNode paths = objectMapper.createObjectNode();
        ObjectNode definitions = objectMapper.createObjectNode();
        doc.set("paths", paths);
        doc.set("definitions", definitions);
        for (String entityName : definitionsManager.getAllKnownDefinitions()) {
            if (Character.isUpperCase(entityName.charAt(0))) {
                ObjectNode path = populateEntityActions(entityName);
                paths.set(String.format("/api/v1/%s/{entityId}", entityName), path);
                JsonNode schemaDefinition = objectMapper.reader().readTree(definitionsManager.getDefinition(entityName).getContent());
                deleteAll$Ids((ObjectNode) schemaDefinition);
//                definitions.set(entityName, schemaDefinition.get("definitions").get(entityName));
                for (Iterator<String> it = schemaDefinition.get("definitions").fieldNames(); it.hasNext(); ) {
                    String fieldName = it.next();
                    definitions.set(fieldName, schemaDefinition.get("definitions").get(fieldName));
                }
            }
        }
        return new ResponseEntity<>(objectMapper.writeValueAsString(doc), HttpStatus.OK);
    }
    @RequestMapping(value = "/api/docs/{file}.json", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Object> getSwaggerDocImportFiles(
            @PathVariable String file
    ) throws IOException {
        Definition definition = definitionsManager.getDefinition(file);
        if (definition == null)
            definition = definitionsManager.getDefinition(file.toLowerCase());
        String content = definition.getContent();
        String inlined = importAllReferences(content);
        return new ResponseEntity<>(inlined, HttpStatus.OK);
    }

    private String importAllReferences(String content) {
        return content;
    }

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

    ObjectNode populateEntityActions(String entityName) throws IOException {
        ObjectNode path = objectMapper.createObjectNode();
        addGetOperation(entityName, path);
        addModifyOperation(entityName, path, "post", "Create");
        addModifyOperation(entityName, path, "put", "Update");
        return path;
    }

    private void addModifyOperation(String entityName, ObjectNode path, String operationType, String descriptionPrefix) throws IOException {
        Operation operation = new Operation()
                .description(String.format("%s new %s", descriptionPrefix, entityName));
        BodyParameter bodyParameter = new BodyParameter()
                .name("entityId")
                .description(String.format("Id of the %s", entityName))
                .schema(new RefModel(String.format("#/definitions/%s", entityName)));
        PathParameter pathParameter = new PathParameter()
                .name("entityId")
                .description(String.format("Id of the %s", entityName))
                .required(true)
                .type("string");
        addResponseType(entityName, path,
                operation.parameter(bodyParameter).parameter(pathParameter), operationType);
    }

    private void addGetOperation(String entityName, ObjectNode path) throws IOException {
        Operation operation = new Operation();
        PathParameter parameter = new PathParameter().name("entityId")
                .description(String.format("Id of the %s", entityName))
                .type("string");
        addResponseType(entityName, path, operation.parameter(parameter), "get");
    }

    private void addResponseType(String entityName, ObjectNode path, Operation operation, String operationType) throws IOException {
        ObjectProperty schema = new ObjectProperty();
        schema.setType("object");
        io.swagger.models.Response response = new io.swagger.models.Response()
                .description("OK")
                .responseSchema(new RefModel(String.format("#/definitions/%s", entityName)));

        operation.addResponse("200", response);

        ObjectNode jsonOperationMapping = (ObjectNode) objectMapper.reader().readTree(Json.mapper().writeValueAsString(operation));
        deleteAll$Ids(jsonOperationMapping);
        path.set(operationType, jsonOperationMapping);
    }

    private void deleteAll$Ids(ObjectNode jsonOperationMapping) {
        if (jsonOperationMapping.has("$id"))
            jsonOperationMapping.remove("$id");
        if (jsonOperationMapping.has("$comment"))
            jsonOperationMapping.remove("$comment");
        jsonOperationMapping.forEach(x-> {
            if (x instanceof ObjectNode) {
                deleteAll$Ids((ObjectNode) x);
            }
        });
    }


}
