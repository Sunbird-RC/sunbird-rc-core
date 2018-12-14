package io.opensaber.registry.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.dao.TPGraphMain;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.exception.TypeNotProvidedException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.Constants.Direction;
import io.opensaber.registry.middleware.util.Constants.JsonldConstants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.service.RegistryAuditService;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.service.SearchService;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.DatabaseProviderWrapper;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.registry.transform.Configuration;
import io.opensaber.registry.transform.ConfigurationHelper;
import io.opensaber.registry.transform.Data;
import io.opensaber.registry.transform.ITransformer;
import io.opensaber.registry.transform.TransformationException;
import io.opensaber.registry.transform.Transformer;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


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
	private RegistryAuditService registryAuditService;
	@Autowired
	private SearchService searchService;
	@Value("${registry.context.base}")
	private String registryContext;
	@Autowired
	private APIMessage apiMessage;
	@Autowired
	private DatabaseProviderWrapper databaseProviderWrapper;

	private Gson gson = new Gson();
	private Type mapType = new TypeToken<Map<String, Object>>() {
	}.getType();
	@Value("${audit.enabled}")
	private boolean auditEnabled;
	@Autowired
	private OpenSaberInstrumentation watch;
	private List<String> keyToPurge = new java.util.ArrayList<>();

	@Autowired
	ShardManager shardManager;

	/**
	 * Note: Only one mime type is supported at a time. Pick up the first mime
	 * type from the header.
	 *
	 * @return
	 */
	@RequestMapping(value = "/search", method = RequestMethod.POST)
	public ResponseEntity<Response> searchEntity(@RequestHeader HttpHeaders header) {

		Model rdf = (Model) apiMessage.getLocalMap(Constants.RDF_OBJECT);
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.SEARCH, "OK", responseParams);
		Map<String, Object> result = new HashMap<>();

		try {
			watch.start("RegistryController.searchEntity");
			String jenaJson = searchService.searchFramed(rdf);
			Data<Object> data = new Data<>(jenaJson);
			Configuration config = configurationHelper.getConfiguration(header.getAccept().iterator().next().toString(),
					Direction.OUT);

			ITransformer<Object> responseTransformer = transformer.getInstance(config);
			responseTransformer.setPurgeData(getKeysToPurge());
			Data<Object> resultContent = responseTransformer.transform(data);
			response.setResult(resultContent.getData());
			responseParams.setStatus(Response.Status.SUCCESSFUL);
			watch.stop("RegistryController.searchEntity");
		} catch (AuditFailedException | RecordNotFoundException | TypeNotProvidedException
				| TransformationException e) {
			logger.error(
					"AuditFailedException | RecordNotFoundException | TypeNotProvidedException in controller while adding entity !",
					e);
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception in controller while searching entities !", e);
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public ResponseEntity<Response> update() {
		Model rdf = (Model) apiMessage.getLocalMap(Constants.RDF_OBJECT);
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);

		try {
			watch.start("RegistryController.update");
			registryService.updateEntity(rdf);
			responseParams.setErrmsg("");
			responseParams.setStatus(Response.Status.SUCCESSFUL);
			watch.stop("RegistryController.update");
			logger.debug("RegistryController: entity updated !");
		} catch (RecordNotFoundException | EntityCreationException e) {
			logger.error(
					"RegistryController: RecordNotFoundException|EntityCreationException while updating entity (without id)!",
					e);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());

		} catch (Exception e) {
			logger.error("RegistryController: Exception while updating entity (without id)!", e);
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
			HealthCheckResponse healthCheckResult = registryService.health();
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
	@RequestMapping(value = "/fetchAudit/{id}", method = RequestMethod.GET)
	public ResponseEntity<Response> fetchAudit(@PathVariable("id") String id) {
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.AUDIT, "OK", responseParams);

		if (auditEnabled) {
			String entityId = registryContext + id;

			try {
				watch.start("RegistryController.fetchAudit");
				org.eclipse.rdf4j.model.Model auditModel = registryAuditService.getAuditNode(entityId);
				logger.debug("Audit Record model :" + auditModel);
				String jenaJSON = registryAuditService.frameAuditEntity(auditModel);
				response.setResult(gson.fromJson(jenaJSON, mapType));
				responseParams.setStatus(Response.Status.SUCCESSFUL);
				watch.stop("RegistryController.fetchAudit");
				logger.debug("Controller: audit records fetched !");
			} catch (RecordNotFoundException e) {
				logger.error("Controller: RecordNotFoundException while fetching audit !", e);
				response.setResult(null);
				responseParams.setStatus(Response.Status.UNSUCCESSFUL);
				responseParams.setErrmsg(e.getMessage());
			} catch (Exception e) {
				logger.error("Controller: Exception while fetching audit !", e);
				response.setResult(null);
				responseParams.setStatus(Response.Status.UNSUCCESSFUL);
				responseParams.setErrmsg("Meh ! You encountered an error!");
			}
		} else {
			logger.info("Controller: Audit is disabled");
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(Constants.AUDIT_IS_DISABLED);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<Response> deleteEntity(@PathVariable("id") String id) {
		String entityId = registryContext + id;
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.DELETE, "OK", responseParams);
		try {
			registryService.deleteEntityById(entityId);
			responseParams.setErrmsg("");
			responseParams.setStatus(Response.Status.SUCCESSFUL);
		} catch (UnsupportedOperationException e) {
			logger.error("Controller: UnsupportedOperationException while deleting entity !", e);
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (RecordNotFoundException e) {
			logger.error("Controller: RecordNotFoundException while deleting entity !", e);
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
	public ResponseEntity<Response> addTP2Graph(@RequestParam(value = "id", required = false) String id,
			@RequestParam(value = "prop", required = false) String property) {

		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.CREATE, "OK", responseParams);
		Map<String, Object> result = new HashMap<>();
		String jsonString = apiMessage.getRequest().getRequestMapAsString();
		String entityType = apiMessage.getRequest().getEntityType();

		try {
			if (shardManager.getShardProperty().compareToIgnoreCase(Constants.NONE_STR) != 0) {
				int slNum = (int) ((HashMap<String, Object>) apiMessage.getRequest().getRequestMap().get(entityType))
						.get(shardManager.getShardProperty());
				databaseProviderWrapper.setDatabaseProvider(shardManager.getDatabaseProvider(slNum));
			} else {
				databaseProviderWrapper.setDatabaseProvider(shardManager.getDefaultDatabaseProvider());
			}

			watch.start("RegistryController.addToExistingEntity");
			String resultId = registryService.addEntity("shard1", jsonString);
			Map resultMap = new HashMap();
			resultMap.put("id", resultId);

			result.put("entity", resultMap);
			response.setResult(result);
			responseParams.setStatus(Response.Status.SUCCESSFUL);
			watch.stop("RegistryController.addToExistingEntity");
			logger.debug("RegistryController : Entity with label {} added !", "");
		} catch (Exception e) {
			logger.error("Exception in controller while adding entity !", e);
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/read", method = RequestMethod.POST)
	public ResponseEntity<Response> greadGraph2Json(@RequestHeader HttpHeaders header) throws Exception {
		String dataObject = apiMessage.getRequest().getRequestMapAsString();
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(dataObject);
		String osIdVal = json.get("id").toString();
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.READ, "OK", responseParams);

		// TODO: The shard id must be fetched from the cache for this uuid and
		// supplied.
		databaseProviderWrapper.setDatabaseProvider(shardManager.getDefaultDatabaseProvider());

		try {
			response.setResult(registryService.getEntity(osIdVal));
		} catch (Exception e) {
			responseParams.setErr(e.getMessage());
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	/*
	 * To set the keys(like @type to be trim of a json
	 */
	private List<String> getKeysToPurge() {
		keyToPurge.add(JsonldConstants.TYPE);
		return keyToPurge;

	}

	private Vertex parentVertex() {
		Vertex parentV = null;
		try {
			DatabaseProvider databaseProvider = databaseProviderWrapper.getDatabaseProvider();
			Graph g = databaseProvider.getGraphStore();

			// TODO: Apply default grouping - to be removed.
			Transaction tx = databaseProvider.startTransaction(g);
			parentV = new TPGraphMain().ensureParentVertex(g, "Teacher_GROUP");
			databaseProvider.commitTransaction(g, tx);

		} catch (Exception e) {
			logger.info(e.getMessage());
		}

		return parentV;
	}

}
