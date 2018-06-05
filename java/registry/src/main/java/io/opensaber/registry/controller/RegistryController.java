package io.opensaber.registry.controller;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import io.opensaber.pojos.*;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.util.JSONUtil;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.service.RegistryService;

@RestController
public class RegistryController {

	private static Logger logger = LoggerFactory.getLogger(RegistryController.class);

	@Autowired
	private RegistryService registryService;

	@Value("${registry.context.base}")
	private String registryContext;

	private Gson gson = new Gson();
	private Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
	
	@Value("${audit.enabled}")
	private boolean auditEnabled;

	@Autowired
	private OpenSaberInstrumentation watch;

	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public ResponseEntity<Response> addToExistingEntity(@RequestAttribute Request requestModel, 
			@RequestParam(value="id", required = false) String id, @RequestParam(value="prop", required = false) String property) {
		watch.start("ADD entity Performance Monitoring !");

		Model rdf = (Model) requestModel.getRequestMap().get("rdf");
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.CREATE, "OK", responseParams);
		Map<String, Object> result = new HashMap<>();

		try {
			String label = registryService.addEntity(rdf, id, property);
			result.put("entity", label);
			response.setResult(result);
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
			logger.debug("RegistryController : Entity with label {} added !", label);
		} catch (DuplicateRecordException | EntityCreationException e) {
			logger.error("DuplicateRecordException|EntityCreationException in controller while adding entity !",e);
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception in controller while adding entity !",e);
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		}
		watch.stop();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ResponseEntity<Response> readEntity(@PathVariable("id") String id) {
		watch.start("READ entity Performance Monitoring ! ");
		String entityId = registryContext + id;
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.READ, "OK", responseParams);

		try {
			org.eclipse.rdf4j.model.Model entityModel = registryService.getEntityById(entityId);
			logger.debug("FETCHED: " + entityModel);
			String jenaJSON = registryService.frameEntity(entityModel);
			response.setResult(gson.fromJson(jenaJSON, mapType));
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
			logger.debug("RegistryController: entity for {} read !", entityId);

		} catch (RecordNotFoundException e) {
			logger.error("RegistryController: RecordNotFoundException while reading entity !", e);
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			logger.error("RegistryController: Exception while reading entity!", e);
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Ding! You encountered an error!");
		}
		watch.stop();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@ResponseBody
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public ResponseEntity<Response> update(@RequestAttribute Request requestModel) {
		watch.start("UPDATE entity Performance Monitoring !");
		Model rdf = (Model) requestModel.getRequestMap().get("rdf");
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);

		try {
			registryService.updateEntity(rdf);
			responseParams.setErrmsg("");
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
			logger.debug("RegistryController: entity updated !");
		} catch (RecordNotFoundException | EntityCreationException e) {
			logger.error("RegistryController: RecordNotFoundException|EntityCreationException while updating entity (without id)!", e);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());

		} catch (Exception e) {
			logger.error("RegistryController: Exception while updating entity (without id)!", e);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Error occurred when updating Entity");
		}
		watch.stop();
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
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
			logger.debug("Application heath checked : ", healthCheckResult.toString());
		} catch (Exception e) {
			logger.error("Error in health checking!", e);
            HealthCheckResponse healthCheckResult =
                    new HealthCheckResponse(Constants.OPENSABER_REGISTRY_API_NAME, false, null);
            response.setResult(JSONUtil.convertObjectJsonMap(healthCheckResult));
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Error during health check");
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/fetchAudit/{id}", method = RequestMethod.GET)
	public ResponseEntity<Response> fetchAudit(@PathVariable("id") String id) {
		watch.start("FETCHAUDIT Performance Monitoring !");
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.AUDIT, "OK", responseParams);

		if (auditEnabled) {
			String entityId = registryContext + id;

			try {
				org.eclipse.rdf4j.model.Model auditModel = registryService.getAuditNode(entityId);
				logger.debug("Audit Record model :" + auditModel);
				String jenaJSON = registryService.frameAuditEntity(auditModel);
				response.setResult(gson.fromJson(jenaJSON, mapType));
				responseParams.setStatus(Response.Status.SUCCCESSFUL);
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
		watch.stop();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
