package io.opensaber.registry.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.opensaber.registry.config.CassandraConfiguration;
import io.opensaber.registry.model.dto.EntityDto;
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
public class RegistryController {
	
	@Autowired
	RegistryService registryService;

	public static void main(String[] args) {
		Class[] classArray = {RegistryController.class,CassandraConfiguration.class};
		SpringApplication.run(classArray, args);
	}
	
	@ResponseBody
	@RequestMapping(value="/getEntityList",method=RequestMethod.GET)
	public ResponseEntity getEntityList() throws JsonProcessingException{
		List entityList = registryService.getEntityList();
		return ResponseUtil.successResponse(entityList);
	}
	
	@ResponseBody
	@RequestMapping(value="/addEntity",method=RequestMethod.POST)
	public ResponseEntity addEntity(@RequestBody Object entity) throws JsonProcessingException{
		boolean status = registryService.addEntity(entity);
		return ResponseUtil.successResponse();
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
