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

public class RegistryIntegrationSteps extends RegistryTestBase{
	
	private static final String VALID_JSONLD= "school.jsonld";
	private static final String INVALID_LABEL_JSONLD = "invalid-label.jsonld";
	private static final String ADD_ENTITY = "addEntity";
	private static final String CONTEXT_CONSTANT = "sample:";
	
	private RestTemplate restTemplate;
	private String baseUrl;
	private ResponseEntity response;
	private Response responseObj;
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
	
	@Given("^an invalid record")
	public void invalidJsonldData(){
		setJsonld(INVALID_LABEL_JSONLD);
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
	public void verifySuccessfulResponse() throws JsonParseException, JsonMappingException, IOException{
		checkSuccessfulResponse();
	}
	
	@Then("^record issuing should be unsuccessful")
	public void verifyUnsuccessfulResponse() throws JsonParseException, JsonMappingException, IOException{
		checkUnsuccessfulResponse();
	}


	private void checkSuccessfulResponse() throws JsonParseException, JsonMappingException, IOException {
		String jsonString = response.getBody().toString();
		responseObj = new ObjectMapper().readValue(jsonString, Response.class);
		assertEquals(Response.Status.SUCCCESSFUL, responseObj.getParams().getStatus());
	}


	private void checkUnsuccessfulResponse() throws JsonParseException, JsonMappingException, IOException {
		String jsonString = response.getBody().toString();
		responseObj = new ObjectMapper().readValue(jsonString, Response.class);
		assertEquals(Response.Status.UNSUCCESSFUL, responseObj.getParams().getStatus());
	}
	
	@Given("(.*) record issued into the registry")
	public void issueRecordInRegistry(String qualifier) throws JsonParseException, JsonMappingException, IOException{
		jsonldData();
		addEntity();
		callRegistryCreateAPI();
		checkSuccessfulResponse();
	}
	
	@Then("^error message is (.*)")
	public void verifyUnsuccessfulMessage(String message){
		assertEquals(message, responseObj.getParams().getErrmsg());
	}
}
