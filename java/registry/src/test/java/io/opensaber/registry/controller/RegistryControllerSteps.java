package io.opensaber.registry.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ObjectNode;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import io.opensaber.registry.util.JsonKeys;

/**
 * 
 * @author jyotsna
 *
 */
public class RegistryControllerSteps {
	
	private RestTemplate restTemplate;
	private String baseUrl;
	private String jsonldString;
	
	
	@Before
	public void initializeData(){
		restTemplate = new RestTemplate();
		baseUrl = generateBaseUrl();
		jsonldString = getJsonldString();
	}
	
	
	@Given("^Input data and base url are valid")
	public void jsonldData(){
		assertNotNull(jsonldString);
		assertNotNull(restTemplate);
		assertNotNull(baseUrl);
	}
	
	@When("^Inserting a record into the registry")
	public void addEntity(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity entity = new HttpEntity(jsonldString,headers);
		ResponseEntity response = restTemplate.postForEntity(baseUrl+"/addEntity",
				entity,ObjectNode.class);
		ObjectNode obj = (ObjectNode)response.getBody();
		assertNotNull(obj);
		assertEquals(obj.get(JsonKeys.RESPONSE).asText(), JsonKeys.SUCCESS);
	}
	
	@Then("^Response for valid record is (.*)")
	public void verifyResponse(String response){
		assertNotNull(response);
		assertTrue(response.contains(JsonKeys.SUCCESS));
	}

	@Given("^Valid duplicate data")
	public void jsonldDuplicateData(){
		assertNotNull(jsonldString);
		assertNotNull(restTemplate);
		assertNotNull(baseUrl);
	}
	
	@When("^Inserting a duplicate record into the registry")
	public void addDuplicateEntity(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity entity = new HttpEntity(jsonldString,headers);
		ResponseEntity response = restTemplate.postForEntity(baseUrl+"/addEntity",
				entity,ObjectNode.class);
		ObjectNode obj = (ObjectNode)response.getBody();
		assertNotNull(obj);
		assertEquals(obj.get(JsonKeys.RESPONSE).asText(), JsonKeys.SUCCESS);
	}
	
	@Then("^Response for duplicate record is (.*)")
	public void verifyFailureResponse(String response){
		assertNotNull(response);
		assertTrue(response.contains(JsonKeys.FAILURE));
	}
	
	@Given("^Input data is invalid")
	public void invalidJsonldData(){
		assertNotNull(getInvalidJsonldString());
		assertNotNull(restTemplate);
		assertNotNull(baseUrl);
	}
	
	@When("^Inserting invalid record into the registry")
	public void addInvalidEntity(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity entity = new HttpEntity(jsonldString,headers);
		ResponseEntity response = restTemplate.postForEntity(baseUrl+"/addEntity",
				entity,ObjectNode.class);
		ObjectNode obj = (ObjectNode)response.getBody();
		assertNotNull(obj);
		assertEquals(obj.get(JsonKeys.RESPONSE).asText(), JsonKeys.SUCCESS);
	}
	
	@Then("^Response for invalid record is (.*)")
	public void verifyFailureResponse2(String response){
		assertNotNull(response);
		assertTrue(response.contains(JsonKeys.FAILURE));
	}

	
	private String getJsonldString(){
		return "{\"@context\": {\"schema\": \"http://schema.org/\",\"opensaber\": \"http://open-saber.org/vocab/core/#\"},\"@type\": "
				+ "[\"schema:Person\",\"opensabre:Teacher\"],\"schema:identifier\": \"b6ad2941-fac3-4c72-94b7-eb638538f55f\",\"schema:image\": null,"
				+ "\"schema:nationality\": \"Indian\",\"schema:birthDate\": \"2011-12-06\",\"schema:name\": \"Marvin\",\"schema:gender\": \"male\","
				+ "\"schema:familyName\":\"Pande\",\"opensaber:languagesKnownISO\": [\"en\",\"hi\"]}";
	}
	
	private String getInvalidJsonldString(){
		return "{\"schema\": \"http://schema.org/\",\"opensaber\": \"http://open-saber.org/vocab/core/#\"},"
				+ "\"schema:identifier\": \"b6ad2941-fac3-4c72-94b7-eb638538f55f\",\"schema:image\": null,"
				+ "\"schema:nationality\": \"Indian\",\"schema:birthDate\": \"2011-12-06\",\"schema:name\": \"Marvin\",\"schema:gender\": \"male\","
				+ "\"schema:familyName\":\"Pande\",\"opensaber:languagesKnownISO\": [\"en\",\"hi\"]";
	}
	
	public String generateBaseUrl(){
		return "http://localhost:8080/registry";
	}

}
