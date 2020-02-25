package io.opensaber.registry.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.helper.RegistryHelper;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.service.IReadService;
import io.opensaber.registry.service.NativeReadService;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.service.ISearchService;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.registry.transform.Configuration;
import io.opensaber.registry.transform.ConfigurationHelper;
import io.opensaber.registry.transform.Data;
import io.opensaber.registry.transform.ITransformer;
import io.opensaber.registry.transform.Transformer;
import io.opensaber.registry.util.RecordIdentifier;
import io.opensaber.registry.util.ViewTemplateManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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
    private ShardManager shardManager;
    
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
     * @param header
     * @return
     */
    @RequestMapping(value = "/read", method = RequestMethod.POST)
    public ResponseEntity<Response> readEntity(@RequestHeader HttpHeaders header) {
        boolean requireLDResponse = header.getAccept().contains(Constants.LD_JSON_MEDIA_TYPE);

        ResponseParams responseParams = new ResponseParams();
        Response response = new Response(Response.API_ID.READ, "OK", responseParams);
        JsonNode inputJson = apiMessage.getRequest().getRequestMapNode();
        try {
            JsonNode resultNode = registryHelper.readEntity(inputJson,apiMessage.getUserID(),requireLDResponse);
            // Transformation based on the mediaType
            Data<Object> data = new Data<>(resultNode);
            Configuration config = configurationHelper.getResponseConfiguration(requireLDResponse);

            ITransformer<Object> responseTransformer = transformer.getInstance(config);
            Data<Object> resultContent = responseTransformer.transform(data);
            response.setResult(resultContent.getData());
            logger.info("ReadEntity,{},{}", resultNode.get(apiMessage.getRequest().getEntityType()).get(uuidPropertyName),config);
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
            registryHelper.updateEntity(inputJson,apiMessage.getUserID());
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
}
