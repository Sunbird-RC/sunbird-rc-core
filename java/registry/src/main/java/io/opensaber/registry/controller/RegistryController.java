package io.opensaber.registry.controller;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import groovy.ui.SystemOutputInterceptor;
import io.opensaber.converters.JenaRDF4J;
import io.opensaber.pojos.Request;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.InvalidTypeException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RegistryService;

@RestController
@SpringBootApplication
@ComponentScan({"io.opensaber.registry"})
public class RegistryController {

	private static Logger logger = LoggerFactory.getLogger(RegistryController.class);

	@Autowired
	private RegistryService registryService;

	@Value("${registry.context.base}")
	private String registryContext;

	public static void main(String[] args) {
		SpringApplication.run(RegistryController.class, args);
	}

	@ResponseBody
	@RequestMapping(value = "/addEntity", method = RequestMethod.POST)
	public ResponseEntity<Response> addEntity(@RequestAttribute Request requestModel)
			throws JsonProcessingException, DuplicateRecordException, InvalidTypeException {

		Response response = new Response();
		ResponseParams responseParams = new ResponseParams();
		response.setId(requestModel.getId());
		response.setEts(System.currentTimeMillis() / 1000L);
		response.setVer("1.0");
		response.setParams(responseParams);

		Model rdf = (Model) requestModel.getRequestMap().get("rdf");
		responseParams.setMsgid(UUID.randomUUID().toString());
		responseParams.setErr("");
		responseParams.setResmsgid("");
		response.setResponseCode("OK");
		Map<String, Object> result = new HashMap<>();
		
		try {
			String label = registryService.addEntity(rdf);			
			result.put("entity", label);
			response.setResult(result);
			responseParams.setErrmsg("");
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
		Response response = new Response();
		ResponseParams responseParams = new ResponseParams();
		response.setEts(System.currentTimeMillis() / 1000L);
		response.setVer("1.0");
		response.setParams(responseParams);		
		id = registryContext + id;
		response.setId(id);
		response.setResponseCode("OK");
		responseParams.setMsgid(UUID.randomUUID().toString());
		responseParams.setErr("");		
		responseParams.setResmsgid("");	
				
		try {			
			org.eclipse.rdf4j.model.Model entityModel = registryService.getEntityById(id);
			logger.info("FETCHED "+entityModel);
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
	@RequestMapping(value = "/entity/{id}", method = RequestMethod.PUT)
	public ResponseEntity<Response> updateEntity(@RequestAttribute Request requestModel, @PathVariable("id") String id) {
		Response response = new Response();
		ResponseParams responseParams = new ResponseParams();		
		response.setEts(System.currentTimeMillis() / 1000L);
		response.setVer("1.0");
		response.setParams(responseParams);

		Model rdf = (Model) requestModel.getRequestMap().get("rdf");
		id = registryContext + id;
		response.setId(id);
		response.setResponseCode("OK");
		responseParams.setMsgid(UUID.randomUUID().toString());
		responseParams.setErr("");		
		responseParams.setResmsgid("");		
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
