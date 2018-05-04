package io.opensaber.registry.controller;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.util.JSONUtil;

import org.apache.jena.rdf.model.Model;
import org.json.JSONObject;
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
	
	@Autowired
	private RegistryService registryService;

	@Value("${registry.context.base}")
	private String registryContext;
	
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
		} catch (DuplicateRecordException | EntityCreationException e) {
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}*/

	
	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public ResponseEntity<Response> addToExistingEntity(@RequestAttribute Request requestModel, 
			@RequestParam(value="id", required = false) String id, @RequestParam(value="prop", required = false) String property) {
		Model rdf = (Model) requestModel.getRequestMap().get("rdf");
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.CREATE, "OK", responseParams);
		Map<String, Object> result = new HashMap<>();

		try {
			String label = registryService.addEntity(rdf, id, property);
			result.put("entity", label);
			response.setResult(result);
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
		} catch (DuplicateRecordException | EntityCreationException e) {
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			response.setResult(result);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		}
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
		} catch (RecordNotFoundException e) {
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Ding! You encountered an error!");
			logger.error("ERROR!", e);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}*/
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ResponseEntity<Response> readEntity(@PathVariable("id") String id) {
		id = registryContext + id;
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.READ, "OK", responseParams);

		try {
			org.eclipse.rdf4j.model.Model entityModel = registryService.getEntityById(id);
			logger.debug("FETCHED: " + entityModel);
			String jenaJSON = registryService.frameEntity(entityModel);
			JSONObject jenaObj = new JSONObject(jenaJSON);
			/*Map<String,Object> resultMap = new HashMap<String,Object>();
			resultMap.put(Constants.RESPONSE_ATTRIBUTE, entityModel);*/
			response.setResult(jenaObj.toMap());
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
		} catch (RecordNotFoundException e) {
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Ding! You encountered an error!");
			logger.error("ERROR!", e);
		}
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
		} catch (RecordNotFoundException | EntityCreationException e) {
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());

		} catch (Exception e) {
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(String.format("Error occurred when updating Entity ID %s", id));
			logger.error("ERROR!", e);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}*/
	
	@ResponseBody
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public ResponseEntity<Response> update(@RequestAttribute Request requestModel) {

		Model rdf = (Model) requestModel.getRequestMap().get("rdf");
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);

		try {
			registryService.updateEntity(rdf);
			responseParams.setErrmsg("");
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
		} catch (RecordNotFoundException | EntityCreationException e) {
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());

		} catch (Exception e) {
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Error occurred when updating Entity");
			logger.error("ERROR!", e);
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
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
		} catch (Exception e) {
            HealthCheckResponse healthCheckResult =
                    new HealthCheckResponse(Constants.OPENSABER_REGISTRY_API_NAME, false, null);
            response.setResult(JSONUtil.convertObjectJsonMap(healthCheckResult));
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Error during health check");
			logger.error("ERROR!", e);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@ResponseBody
	@RequestMapping(value = "/fetchAudit/{id}", method = RequestMethod.GET)
	public ResponseEntity<Response> fetchAudit(@PathVariable("id") String id) {

		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.AUDIT, "OK", responseParams);
		id=registryContext+id;	

		try {
			org.eclipse.rdf4j.model.Model auditModel = registryService.getAuditNode(id);
			String jenaJSON = registryService.frameAuditEntity(auditModel);
			Type type = new TypeToken<Map<String, Object>>(){}.getType();
			response.setResult(new Gson().fromJson(jenaJSON, type));
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
		} catch (RecordNotFoundException e) {
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			response.setResult(null);
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg("Meh ! You encountered an error!");
			logger.error("ERROR!", e);
		}	
		return new ResponseEntity<>(response, HttpStatus.OK);
	}


}
