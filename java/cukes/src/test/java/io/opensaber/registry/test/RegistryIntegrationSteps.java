package io.opensaber.registry.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.opensaber.pojos.Response;


/**
 * 
 * @author jyotsna
 *
 */
public class RegistryIntegrationSteps extends RegistryTestBase{
	
	private static final String VALID_JSONLD= "school.jsonld";
	private static final String INVALID_LABEL_JSONLD = "invalid-label.jsonld";
	private static final String ADD_ENTITY = "addEntity";
	private static final String CONTEXT_CONSTANT = "sample:";
	
	private RestTemplate restTemplate;
	private String baseUrl;
	private ResponseEntity response;
	private static String duplicateLabel;
	
	
	@Before
	public void initializeData(){
		restTemplate = new RestTemplate();
		baseUrl = generateBaseUrl();
	}
	
	
	@Given("^a valid record")
	public void jsonldData(){
		setJsonld(VALID_JSONLD);
		String label = generateRandomId();
		setJsonldWithNewRootLabel(CONTEXT_CONSTANT+label);
		assertNotNull(jsonld);
	}
	
	@When("^issuing the record into the registry")
	public void addEntity(){
		response = callRegistryCreateAPI();
	}

	private ResponseEntity callRegistryCreateAPI() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity entity = new HttpEntity(jsonld,headers);
		ResponseEntity response = restTemplate.postForEntity(
				baseUrl+ADD_ENTITY,
				entity,
				ObjectNode.class);
		return response;
	}
	
	@Then("^record issuing should be successful")
	public void verifyResponse() throws JsonParseException, JsonMappingException, IOException{
		checkSuccessfulResponse();
	}


	private void checkSuccessfulResponse() throws JsonParseException, JsonMappingException, IOException {
		String jsonString = response.getBody().toString();
		Response responseObj = new ObjectMapper().readValue(jsonString, Response.class);
		assertEquals(Response.Status.SUCCCESSFUL, responseObj.getParams().getStatus());
//		Object obj = new JSONParser().parse(response.getBody());
//		assertTrue(response.contains(JsonKeys.SUCCESSFUL));
	}
	
//	@Given("^a record issued into the registry")
//	public void prepareRegistryWithRecord() throws JsonParseException, JsonMappingException, IOException{
//		jsonldData();
//		addEntity();
//		callRegistryCreateAPI();
//		checkSuccessfulResponse();
//	}
//
//	@Given("^Valid duplicate data")
//	public void jsonldDuplicateData(){
//		setJsonld(VALID_JSONLD);
//		assertNotNull(jsonld);
//		assertNotNull(restTemplate);
//		assertNotNull(baseUrl);
//	}
//	
//	@When("^Inserting a duplicate record into the registry")
//	public void addDuplicateEntity(){
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//		setJsonldWithNewRootLabel(CONTEXT_CONSTANT+duplicateLabel);
//		HttpEntity entity = new HttpEntity(jsonld,headers);
//		ResponseEntity response = restTemplate.postForEntity(baseUrl+ADD_ENTITY,
//				entity,ObjectNode.class);
//		ObjectNode obj = (ObjectNode)response.getBody();
//		assertNotNull(obj);
//		assertEquals(obj.get(JsonKeys.RESPONSE).asText(), Constants.DUPLICATE_RECORD_MESSAGE);
//	}
//	
//	@Then("^Response for duplicate record is (.*)")
//	public void verifyFailureResponse(String response){
//		assertNotNull(response);
//		assertTrue(response.contains(Constants.DUPLICATE_RECORD_MESSAGE));
//	}
//	
//	@Given("^Second input data and base url are valid")
//	public void newJsonldData(){
//		setJsonld(VALID_JSONLD);
//		assertNotNull(jsonld);
//		assertNotNull(restTemplate);
//		assertNotNull(baseUrl);
//	}
//	
//	@When("^Inserting second valid record into the registry")
//	public void addNewEntity(){
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//		String label = generateRandomId();
//		setJsonldWithNewRootLabel(CONTEXT_CONSTANT + label);
//		HttpEntity entity = new HttpEntity(jsonld,headers);
//		ResponseEntity response = restTemplate.postForEntity(baseUrl+ADD_ENTITY,
//				entity,ObjectNode.class);
//		ObjectNode obj = (ObjectNode)response.getBody();
//		assertNotNull(obj);
//		assertEquals(obj.get(JsonKeys.RESPONSE).asText(), JsonKeys.SUCCESSFUL);
//	}
//	
//	@Then("^Response for second valid record is (.*)")
//	public void verifyResponse2(String response){
//		assertNotNull(response);
//		assertTrue(response.contains(JsonKeys.SUCCESSFUL));
//	}
//	
//	@Given("^Base url is valid but input data has invalid type")
//	public void invalidJsonldData(){
//		setJsonld(INVALID_LABEL_JSONLD);
//		assertNotNull(jsonld);
//		assertNotNull(restTemplate);
//		assertNotNull(baseUrl);
//	}
//	
//	@When("^Inserting record with invalid type into the registry")
//	public void addInvalidEntity(){
//		ResponseEntity obj = callRegistryCreateAPI();
//		assertNotNull(obj);
////		assertEquals(obj.get(JsonKeys.RESPONSE).asText(), Constants.INVALID_TYPE_MESSAGE);
//	}
//	
//	@Then("^Response for invalid record is (.*)")
//	public void verifyFailureResponse2(String response){
//		assertNotNull(response);
//		assertTrue(response.contains(Constants.INVALID_TYPE_MESSAGE));
//	}
}
