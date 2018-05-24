package io.opensaber.registry.controller;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.util.JSONUtil;

import javafx.scene.paint.Stop;
import org.apache.jena.rdf.model.Model;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.opensaber.pojos.Request;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.service.RegistryService;

@RestController
public class RegistryController {

	private static Logger logger = LoggerFactory.getLogger(RegistryController.class);
	private static Logger prefLogger = LoggerFactory.getLogger("PERFORMANCE_INSTRUMENTATION");
	
	@Autowired
	private RegistryService registryService;

	@Value("${registry.context.base}")
	private String registryContext;

	StopWatch watch =new StopWatch();
	
	/*@RequestMapping(value = "/create", method = RequestMethod.POST)
	public ResponseEntity<Response> addEntity(@RequestAttribute Request requestModel) {
		Model rdf = (Model) requestModel.getRequestMap().get("rdf");
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.CREATE, "OK", responseParams);
		Map<String, Object> result = new HashMap<>();

		try {
			String label = registryService.addEntity(rdf);
			result.put("entity", label);
			response.setResult(result);
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
			logger.debug("Controller : Entity with label {} created !", label);
		} catch (DuplicateRecordException | EntityCreationException e) {
			logger.error("Controller : DuplicateRecordException|EntityCreationException while creating entity !", e);
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			logger.error("Controller: Exception while creating entity !", e);
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}*/

	
	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public ResponseEntity<Response> addToExistingEntity(@RequestAttribute Request requestModel, 
			@RequestParam(value="id", required = false) String id, @RequestParam(value="prop", required = false) String property) {
		watch.start("ADD Performance Monitoring !");
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
		prefLogger.info(watch.prettyPrint());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	/*@RequestMapping(value = "/read/{id}", method = RequestMethod.GET)
	public ResponseEntity<Response> getEntity(@PathVariable("id") String id) {
		id = registryContext + id;
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.READ, "OK", responseParams);

		try {
			org.eclipse.rdf4j.model.Model entityModel = registryService.getEntityById(id);
			logger.debug("FETCHED: " + entityModel);
			String jenaJSON = registryService.frameEntity(entityModel);
			JSONObject jenaObj = new JSONObject(jenaJSON);
			Map<String,Object> resultMap = new HashMap<String,Object>();
			resultMap.put(Constants.RESPONSE_ATTRIBUTE, entityModel);
			response.setResult(jenaObj.toMap());
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
			logger.debug("Controller: entity for {} fetched !", id);
		} catch (RecordNotFoundException e) {
			logger.error("Controller: RecordNotFoundException while fetching entity !", e);
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			logger.error("Controller: Exception while fetching entity!", e);
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Ding! You encountered an error!");
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}*/
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ResponseEntity<Response> readEntity(@PathVariable("id") String id) {
		watch.start("READ Performance Monitoring ! ");
		id = registryContext + id;
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.READ, "OK", responseParams);

		try {
			org.eclipse.rdf4j.model.Model entityModel = registryService.getEntityById(id);
			logger.debug("RegistryController : Fetched entity: " + entityModel);
			String jenaJSON = registryService.frameEntity(entityModel);
			JSONObject jenaObj = new JSONObject(jenaJSON);
			/*Map<String,Object> resultMap = new HashMap<String,Object>();
			resultMap.put(Constants.RESPONSE_ATTRIBUTE, entityModel);*/
			response.setResult(jenaObj.toMap());
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
			logger.debug("RegistryController: Read entity for {}!", id);
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
		prefLogger.info(watch.prettyPrint());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	/*@ResponseBody
	@RequestMapping(value = "/update/{id}", method = RequestMethod.PATCH)
	public ResponseEntity<Response> updateEntity(@RequestAttribute Request requestModel,
			@PathVariable("id") String id) {

		Model rdf = (Model) requestModel.getRequestMap().get("rdf");
		id = registryContext + id;
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);

		try {
			registryService.updateEntity(rdf, id);
			responseParams.setErrmsg("");
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
			logger.debug("Controller: entity for {} updated !", id);
		} catch (RecordNotFoundException | EntityCreationException e) {
			logger.error("Controller: RecordNotFoundException|EntityCreationException while updating entity !", e);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());

		} catch (Exception e) {
			logger.error("Controller: Exception while updating entity!", e);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(String.format("Error occurred when updating Entity ID {}", id));
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}*/
	
	@ResponseBody
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public ResponseEntity<Response> update(@RequestAttribute Request requestModel) {
		watch.start("UPDATE Performance Monitoring !");
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
		prefLogger.info(watch.prettyPrint());
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
		id=registryContext+id;	

		try {
			org.eclipse.rdf4j.model.Model auditModel = registryService.getAuditNode(id);
			logger.debug("RegistryController : Audit Record model :"+ auditModel);
			String jenaJSON = registryService.frameAuditEntity(auditModel);
			Type type = new TypeToken<Map<String, Object>>(){}.getType();
			response.setResult(new Gson().fromJson(jenaJSON, type));
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
			logger.debug("RegistryController: audit records fetched !");
		} catch (RecordNotFoundException e) {
			logger.error("RegistryController: RecordNotFoundException while fetching audit !", e);

			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			logger.error("RegistryController: Exception while fetching audit !", e);
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Meh ! You encountered an error!");
		}
		watch.stop();
		prefLogger.info(watch.prettyPrint());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
