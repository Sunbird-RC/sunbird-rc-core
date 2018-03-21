package io.opensaber.registry.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.opensaber.pojos.Request;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.InvalidTypeException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.service.RegistryService;

@RestController
public class RegistryController {

	private static Logger logger = LoggerFactory.getLogger(RegistryController.class);

	@Autowired
	private RegistryService registryService;

	@Value("${registry.context.base}")
	private String registryContext;

	@ResponseBody
	@RequestMapping(value = "/addEntity", method = RequestMethod.POST)
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
		} catch (DuplicateRecordException | InvalidTypeException e) {
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

	@RequestMapping(value = "/getEntity/{id}", method = RequestMethod.GET)
	public ResponseEntity<Response> getEntity(@PathVariable("id") String id){
		id = registryContext + id;
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.READ, "OK", responseParams);
				
		try {			
			org.eclipse.rdf4j.model.Model entityModel = registryService.getEntityById(id);
			logger.debug("FETCHED " + entityModel);
			String jenaJSON = registryService.frameEntity(entityModel);
			JSONObject jenaObj=new JSONObject(jenaJSON);
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

	@ResponseBody
	@RequestMapping(value = "/entity/{id}", method = RequestMethod.PATCH)
	public ResponseEntity<Response> updateEntity(@RequestAttribute Request requestModel, @PathVariable("id") String id) {

		Model rdf = (Model) requestModel.getRequestMap().get("rdf");
		id = registryContext + id;
		ResponseParams responseParams = new ResponseParams();
		Response response = new Response(Response.API_ID.UPDATE, "OK", responseParams);

		try {			
			registryService.updateEntity(rdf, id);			
			responseParams.setErrmsg("");
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
		} catch (RecordNotFoundException | InvalidTypeException e) {
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
			
		} catch (Exception e) {
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(String.format("Error occurred when updating Entity ID %s", id));
			logger.error("ERROR!", e);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
