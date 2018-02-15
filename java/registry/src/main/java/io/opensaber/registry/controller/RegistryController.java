package io.opensaber.registry.controller;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.util.ResponseUtil;

/**
 * 
 * @author jyotsna
 *
 */
@Controller
@SpringBootApplication
@ComponentScan({"io.opensaber.registry"})
public class RegistryController extends SpringBootServletInitializer {

	@Autowired
	RegistryService registryService;

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(RegistryController.class);
	}

	@ResponseBody
	@RequestMapping(value="/getEntityList",method=RequestMethod.GET)
	public ResponseEntity getEntityList() throws JsonProcessingException{
		List entityList = registryService.getEntityList();
		return ResponseUtil.successResponse(entityList);
	}

	@ResponseBody
	@RequestMapping(value="/addEntity",method=RequestMethod.POST)
	public ResponseEntity addEntity(@RequestAttribute Object rdf) throws JsonProcessingException, NullPointerException, DuplicateRecordException{
		try{
			boolean status = registryService.addEntity(rdf);
			if(status){
				return ResponseUtil.successResponse();
			}else{
				return ResponseUtil.failureResponse(Constants.FAILED_INSERTION_MESSAGE);
			}
		}catch(NullPointerException e){
			e.printStackTrace();
			return ResponseUtil.failureResponse(Constants.FAILED_INSERTION_MESSAGE);
		}catch(Exception e){
			e.printStackTrace();
			return ResponseUtil.failureResponse(e.getMessage());
		}

	}

	@ResponseBody
	@RequestMapping(value="/updateEntity",method=RequestMethod.PUT)
	public ResponseEntity updateEntity(@RequestBody Object entity) throws JsonProcessingException{
		boolean status = registryService.updateEntity(entity);
		return ResponseUtil.successResponse();
	}

	@ResponseBody
	@RequestMapping(value="/deleteEntity",method=RequestMethod.POST)
	public ResponseEntity deleteEntity(@RequestBody Object entity) throws JsonProcessingException{
		boolean status = registryService.deleteEntity(entity);
		return ResponseUtil.successResponse();
	}

	@ResponseBody
	@RequestMapping(value="/getEntity",method=RequestMethod.POST)
	public ResponseEntity getEntity(@RequestBody Object entity) throws JsonProcessingException{
		Object responseObj = registryService.getEntityById(entity);
		return ResponseUtil.successResponse(responseObj);
	}

}
