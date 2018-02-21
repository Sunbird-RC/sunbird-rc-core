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
import io.opensaber.registry.middleware.util.Constants;

/**
 * 
 * @author jyotsna
 *
 */
public class RegistryControllerSteps extends RegistryTestBase{
	
	private static final String VALID_JSONLD1 = "school1.jsonld";
	/*private static final String VALID_JSONLD2 = "school2.jsonld";*/
	private static final String INVALID_LABEL_JSONLD = "invalid-label.jsonld";
	
	private RestTemplate restTemplate;
	private String baseUrl;
	private String duplicateLabel;
	
	
	@Before
	public void initializeData(){
		restTemplate = new RestTemplate();
		baseUrl = generateBaseUrl();
	}
	
	
	@Given("^First input data and base url are valid")
	public void jsonldData(){
		setJsonld(VALID_JSONLD1);
		assertNotNull(jsonld);
		assertNotNull(restTemplate);
		assertNotNull(baseUrl);
	}
	
	@When("^Inserting first valid record into the registry")
	public void addEntity(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String label = generateRandomId();
		duplicateLabel = label;
		setJsonldWithNewRootLabel(label);
		HttpEntity entity = new HttpEntity(jsonld,headers);
		ResponseEntity response = restTemplate.postForEntity(baseUrl+"/addEntity",
				entity,ObjectNode.class);
		ObjectNode obj = (ObjectNode)response.getBody();
		assertNotNull(obj);
		assertEquals(obj.get(JsonKeys.RESPONSE).asText(), JsonKeys.SUCCESS);
	}
	
	@Then("^Response for first valid record is (.*)")
	public void verifyResponse(String response){
		assertNotNull(response);
		assertTrue(response.contains(JsonKeys.SUCCESS));
	}

	@Given("^Valid duplicate data")
	public void jsonldDuplicateData(){
		setJsonld(VALID_JSONLD1);
		assertNotNull(jsonld);
		assertNotNull(restTemplate);
		assertNotNull(baseUrl);
	}
	
	@When("^Inserting a duplicate record into the registry")
	public void addDuplicateEntity(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		setJsonldWithNewRootLabel(duplicateLabel);
		HttpEntity entity = new HttpEntity(jsonld,headers);
		ResponseEntity response = restTemplate.postForEntity(baseUrl+"/addEntity",
				entity,ObjectNode.class);
		ObjectNode obj = (ObjectNode)response.getBody();
		assertNotNull(obj);
		assertEquals(obj.get(JsonKeys.RESPONSE).asText(), Constants.DUPLICATE_RECORD_MESSAGE);
	}
	
	@Then("^Response for duplicate record is (.*)")
	public void verifyFailureResponse(String response){
		assertNotNull(response);
		assertTrue(response.contains(Constants.DUPLICATE_RECORD_MESSAGE));
	}
	
	@Given("^Second input data and base url are valid")
	public void newJsonldData(){
		setJsonld(VALID_JSONLD1);
		assertNotNull(jsonld);
		assertNotNull(restTemplate);
		assertNotNull(baseUrl);
	}
	
	@When("^Inserting second valid record into the registry")
	public void addNewEntity(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String label = generateRandomId();
		setJsonldWithNewRootLabel(label);
		HttpEntity entity = new HttpEntity(jsonld,headers);
		ResponseEntity response = restTemplate.postForEntity(baseUrl+"/addEntity",
				entity,ObjectNode.class);
		ObjectNode obj = (ObjectNode)response.getBody();
		assertNotNull(obj);
		assertEquals(obj.get(JsonKeys.RESPONSE).asText(), JsonKeys.SUCCESS);
	}
	
	@Then("^Response for second valid record is (.*)")
	public void verifyResponse2(String response){
		assertNotNull(response);
		assertTrue(response.contains(JsonKeys.SUCCESS));
	}
	
	@Given("^Base url is valid but input data has invalid root label")
	public void invalidJsonldData(){
		setJsonld(INVALID_LABEL_JSONLD);
		assertNotNull(jsonld);
		assertNotNull(restTemplate);
		assertNotNull(baseUrl);
	}
	
	@When("^Inserting record with invalid type into the registry")
	public void addInvalidEntity(){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String label = generateRandomId();
		setJsonldWithNewRootLabel(label);
		HttpEntity entity = new HttpEntity(jsonld,headers);
		ResponseEntity response = restTemplate.postForEntity(baseUrl+"/addEntity",
				entity,ObjectNode.class);
		ObjectNode obj = (ObjectNode)response.getBody();
		assertNotNull(obj);
		assertEquals(obj.get(JsonKeys.RESPONSE).asText(), Constants.FAILED_INSERTION_MESSAGE);
	}
	
	@Then("^Response for invalid record is (.*)")
	public void verifyFailureResponse2(String response){
		assertNotNull(response);
		assertTrue(response.contains(Constants.FAILED_INSERTION_MESSAGE));
	}
}
