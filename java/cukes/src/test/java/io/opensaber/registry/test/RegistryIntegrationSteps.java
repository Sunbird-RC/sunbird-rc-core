package io.opensaber.registry.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.json.JSONObject;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cucumber.api.PendingException;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.opensaber.pojos.Response;

public class RegistryIntegrationSteps extends RegistryTestBase{
	
	private static final String VALID_JSONLD= "school.jsonld";
	private static final String VALID_NEW_JSONLD="newSchool.jsonld";
	private static final String INVALID_LABEL_JSONLD = "invalid-label.jsonld";
	private static final String ADD_ENTITY = "addEntity";
	private static final String CONTEXT_CONSTANT = "sample:";
	private static final String READ_ENTITY = "getEntity";
	
	private RestTemplate restTemplate;
	private String baseUrl;
	private ResponseEntity<ObjectNode> response;
	private Response responseObj;
	private String labelToFetch;
	private String label;
	private static String duplicateLabel;
	
	
	@Before
	public void initializeData(){
		restTemplate = new RestTemplate();
		baseUrl = generateBaseUrl();
	}
	
	/*@Given("^a valid record")
	public void jsonldData(){
		setJsonld(VALID_JSONLD);
		label = generateRandomId();
		setJsonldWithNewRootLabel(CONTEXT_CONSTANT+label);
		assertNotNull(jsonld);
	}*/
	
	@Given("^a valid record")
	public void jsonldData(){
		setJsonld(VALID_NEW_JSONLD);
		label = generateRandomId();
		setJsonldWithNewRootLabel(CONTEXT_CONSTANT+label);
		assertNotNull(jsonld);
	}
	
	@Given("^an invalid record")
	public void invalidJsonldData(){
		setJsonld(INVALID_LABEL_JSONLD);
		label = generateRandomId();
		setJsonldWithNewRootLabel(CONTEXT_CONSTANT+label);
		assertNotNull(jsonld);
	}
	
	@When("^issuing the record into the registry")
	public void addEntity(){
		response = callRegistryCreateAPI();
	}

	private ResponseEntity<ObjectNode> callRegistryCreateAPI() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonld,headers);
		ResponseEntity<ObjectNode> response = restTemplate.postForEntity(
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
		checkSuccessfulResponse();
	}
	
	@Then("^error message is (.*)")
	public void verifyUnsuccessfulMessage(String message){
		assertEquals(message, responseObj.getParams().getErrmsg());
	}
	
	@Given("^a non existent record id$")
	public void a_non_existent_record_id() throws Exception {
		label = generateRandomId();
	}

	@When("^retrieving the record from the registry$")
	public void retrieving_the_record_from_the_registry(){
		response = callRegistryReadAPI();
	}

	private ResponseEntity<ObjectNode> callRegistryReadAPI() {
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<String>("",headers);
		ResponseEntity<ObjectNode> response = restTemplate.getForEntity(baseUrl+READ_ENTITY+"/"+label, ObjectNode.class);
//		ResponseEntity response = restTemplate.postForEntity(
//				baseUrl+READ_ENTITY,
//				entity,
//				ObjectNode.class);
		return response;
		
	}

	@Then("^record retrieval should be unsuccessful$")
	public void record_retrieval_should_be_unsuccessful() throws Exception {
		checkUnsuccessfulResponse();
	}
	
	@Given("^an existent record id$")
	public void an_existent_record_id() throws Exception {
		jsonldData();
		addEntity();
		checkSuccessfulResponse();
	}

	@Then("^record retrieval should be successful$")
	public void record_retrieval_should_be_successful() throws Exception {
		checkSuccessfulResponse();
	}

	@Then("^the record should match$")
	public void the_record_should_match() throws Exception {
		Model expectedModel = ModelFactory.createDefaultModel();
		RDFDataMgr.read(expectedModel, new StringReader(jsonld), null, org.apache.jena.riot.RDFLanguages.JSONLD) ;
		Map<String, Object> result = responseObj.getParams().getResultMap();
		Model actualModel = ModelFactory.createDefaultModel();
		String newJsonld = new JSONObject(result).toString(2);
		RDFDataMgr.read(actualModel, new StringReader(newJsonld), null, org.apache.jena.riot.RDFLanguages.JSONLD);
		assertTrue(expectedModel.isIsomorphicWith(actualModel));
//		System.out.println(object.keySet().size());
	}
}
