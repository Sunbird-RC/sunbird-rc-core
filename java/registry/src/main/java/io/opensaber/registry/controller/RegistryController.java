package io.opensaber.registry.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.net.ssl.SSLEngineResult.Status;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.neo4j.causalclustering.catchup.ResponseMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseParams;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.InvalidTypeException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.util.ResponseUtil;

@RestController
@SpringBootApplication
@ComponentScan({"io.opensaber.registry"})
public class RegistryController extends SpringBootServletInitializer {

	private static Logger logger = LoggerFactory.getLogger(RegistryController.class);

	@Autowired
	RegistryService registryService;

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(RegistryController.class);
	}

	@ResponseBody
	@RequestMapping(value="/addEntity",method=RequestMethod.POST)
	public ResponseEntity<Response> addEntity(@RequestAttribute Model rdf) throws JsonProcessingException, DuplicateRecordException, InvalidTypeException{
		Response response = new Response();
		ResponseParams responseParams = new ResponseParams();
		response.setId(UUID.randomUUID().toString());
		response.setEts(System.currentTimeMillis() / 1000L);
		response.setVer("1.0");
		response.setParams(responseParams);
		try{
			registryService.addEntity(rdf);
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
		} catch (DuplicateRecordException e) {
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (InvalidTypeException e) {
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		} catch (Exception e) {
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/getEntity/{id}", method = RequestMethod.GET)
	public ResponseEntity<Response> getEntity(@PathVariable("id") String id){
		Response response = new Response();
		ResponseParams responseParams = new ResponseParams();
		response.setId(UUID.randomUUID().toString());
		response.setEts(System.currentTimeMillis() / 1000L);
		response.setVer("1.0");
		response.setParams(responseParams);
		try {
			org.eclipse.rdf4j.model.Model entityModel;
			id = "http://example.com/voc/teacher/1.0.0/" + id;
			entityModel = registryService.getEntityById(id);
			responseParams.setStatus(Response.Status.SUCCCESSFUL);
		} catch (RecordNotFoundException e) {
			responseParams.setStatus(Response.Status.UNSUCCESSFUL);
			responseParams.setErrmsg(e.getMessage());
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

}
