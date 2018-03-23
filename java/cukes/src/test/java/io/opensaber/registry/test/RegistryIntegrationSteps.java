package io.opensaber.registry.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.json.JSONObject;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.Response.Status;

public class RegistryIntegrationSteps extends RegistryTestBase{
	

	private static final String VALID_JSONLD= "school.jsonld";
	//private static final String VALID_NEWJSONLD= "newSchool.jsonld";
	private static final String VALID_NEWJSONLD= "teacher.jsonld";
	private static final String INVALID_LABEL_JSONLD = "invalid-label.jsonld";
	private static final String INVALID_NEWJSONLD= "invalid-teacher.jsonld";
	private static final String ADD_ENTITY = "addEntity";
	private static final String READ_ENTITY = "getEntity";
	private static final String UPDATE_ENTITY = "entity";
	private static final String AUTH_HEADER_NAME = "x-authenticated-user-token";
		
	private RestTemplate restTemplate;
	private String baseUrl;
	private ResponseEntity<Response> response;
	private Response responseObj;
	private String labelToFetch;
	private String id;
	private static String duplicateLabel;
	private HttpHeaders headers;
	private String updateId;
	
	@Before
	public void initializeData(){
		restTemplate = new RestTemplate();
		baseUrl = generateBaseUrl();
	}
	
	@Given("^a valid record")
	public void jsonldData(){
		setJsonld(VALID_NEWJSONLD);
		id=setJsonldWithNewRootLabel();	
		assertNotNull(jsonld);
	}
	
	@Given("^a record with invalid type")
	public void invalidTypeJsonldData(){
		setJsonld(INVALID_LABEL_JSONLD);
	    id=setJsonldWithNewRootLabel();
		assertNotNull(jsonld);
	}
	
	@And("^an invalid record")
	public void invalidJsonldData(){
		setJsonld(INVALID_NEWJSONLD);
	    id=setJsonldWithNewRootLabel();
		assertNotNull(jsonld);
	}
	
	@And("^a valid auth token")
	public void setValidAuthToken(){
		setValidAuthHeader();
		assertNotNull(headers);
	}
	
	@And("^an invalid auth token")
	public void setInvalidAuthToken(){
		setInvalidAuthHeader();
		assertNotNull(headers);
	}
	
	@And("^a missing auth token")
	public void setMissingAuthToken(){
		headers = new HttpHeaders();
		assertNotNull(headers);
	}
	
	@When("^issuing the record into the registry")
	public void addEntity(){
		response = callRegistryCreateAPI();
	}
	
	@When("^updating the record in registry")
	public void updateEntity(){
		JsonParser p = new JsonParser();
        JsonObject jsonObject = p.parse(jsonld).getAsJsonObject();
        jsonObject.addProperty("@id", id);
		response = callRegistryUpdateAPI();
	}

	private ResponseEntity<Response> callRegistryCreateAPI() {
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(jsonld,headers);
		ResponseEntity<Response> response = restTemplate.postForEntity(
				baseUrl+ADD_ENTITY,
				entity,
				Response.class);	
		return response;
	}
	
	private ResponseEntity<Response> callRegistryUpdateAPI() {
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(jsonld,headers);
		//ResponseEntity<Response> response = restTemplate.exchange(baseUrl+UPDATE_ENTITY+"/"+id, HttpMethod.PATCH,entity,Response.class);
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		restTemplate.setRequestFactory(requestFactory);
		Response response = restTemplate.patchForObject(baseUrl+UPDATE_ENTITY+"/"+updateId, entity, Response.class);
		return new ResponseEntity(response,HttpStatus.OK);
	}
	
	@Then("^record issuing should be successful")
	public void verifySuccessfulResponse() throws JsonParseException, JsonMappingException, IOException{
		checkSuccessfulResponse();
	}
	
	@Then("^record issuing should be unsuccessful")
	public void verifyUnsuccessfulResponse() throws JsonParseException, JsonMappingException, IOException{
		checkUnsuccessfulResponse();
	}
	
	@And("^fetching the record from the registry should match the issued record")
	public void fetchRecordFromRegistryAndVerify() throws JsonParseException, JsonMappingException, IOException{
		setValidAuthToken();
		response = callRegistryReadAPI();
		checkForIsomorphicModel();
	}


	private void checkSuccessfulResponse() throws JsonParseException, JsonMappingException, IOException {
		Status responseStatus = response.getBody().getParams().getStatus();
		assertEquals(Response.Status.SUCCCESSFUL, responseStatus);
	}


	private void checkUnsuccessfulResponse() throws JsonParseException, JsonMappingException, IOException {
		Status responseStatus = response.getBody().getParams().getStatus();
		assertEquals(Response.Status.UNSUCCESSFUL, responseStatus);
	}
	
	private void checkForIsomorphicModel() throws IOException{
		Model expectedModel = ModelFactory.createDefaultModel();
		ObjectMapper mapper = new ObjectMapper();
		String jsonldBody = mapper.readTree(jsonld).path("request").toString();
		RDFDataMgr.read(expectedModel, new StringReader(jsonldBody), null, org.apache.jena.riot.RDFLanguages.JSONLD) ;
		Map<String, Object> result = response.getBody().getResult();
		Model actualModel = ModelFactory.createDefaultModel();
		String newJsonld = new JSONObject(result).toString(2);
		RDFDataMgr.read(actualModel, new StringReader(newJsonld), null, org.apache.jena.riot.RDFLanguages.JSONLD);
		assertTrue(expectedModel.isIsomorphicWith(actualModel));
	}
	
	private void setValidAuthHeader(){
		headers = new HttpHeaders();
		headers.add(AUTH_HEADER_NAME, "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1WXhXdE4tZzRfMld5MG5P"
				+ "S1ZoaE5hU0gtM2lSSjdXU25ibFlwVVU0TFRrIn0.eyJqdGkiOiI2OTBiNDZjZS03MjI5LTQ5NjgtODU4Yy0yMzNjNmJhZjMxODMiLCJleHAiOjE1MjE1NjI0NDUsIm5iZiI6MCwiaWF0IjoxNTIxNTE5MjQ1LCJpc3MiOiJodHRwczovL3N0YWdpbmcub3Blbi1zdW5iaXJkLm9yZy9hdXRoL3"
				+ "JlYWxtcy9zdW5iaXJkIiwiYXVkIjoiYWRtaW4tY2xpIiwic3ViIjoiYWJkYmRjYzEtZDI5Yy00ZTQyLWI1M2EtODVjYTY4NzI3MjRiIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWRtaW4tY2xpIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZmZiYWE2ZWUtMDhmZi00OGVlLThlYTEt"
				+ "ZTI3YzhlZTE5ZDVjIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlc291cmNlX2FjY2VzcyI6e30sIm5hbWUiOiJSYXl1bHUgVmlsbGEiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ2cmF5dWx1IiwiZ2l2ZW5fbmFtZSI6IlJheXVsdSIsImZhbWlseV9uYW1lIjoiVmlsbGEiLCJlbWF"
				+ "pbCI6InJheXVsdUBnbWFpbC5jb20ifQ.U1hsUoXGYKtYssOkytMo_tnexHhwKs86IXrDw8rhL9tpG5c6DArVJvdhn5wTEbgzp52efNwQ5LrGGmpBFRWDw0szA5ggT347RCbTTxXZEFF2bUEE8rr0KbkfPOwk5Gazo_xRerW-URyWPlzqppZaUPc6kzY8TDouGmKF8qyVenaxrRgbhKNRYbZWFviARLyt"
				+ "ZTMLtgLafhmOvj6r3vK-kt36afUNROBSoNaxhcvSF9QnTRB1_0Bnb_qyVMqEDSdwZdGs3rMU_W8SFWMewxxXPuYWEXIvXIr2AMs7naCR4colLGz8AOMFR44-qTEF-eF71qqBNouh1hgd4N0l4sKzxA");
	}
	
	private void setInvalidAuthHeader(){
		headers = new HttpHeaders();
		headers.add(AUTH_HEADER_NAME, "1234");
	}
	
	@Given("(.*) record issued into the registry")
	public void issueRecordInRegistry(String qualifier) throws JsonParseException, JsonMappingException, IOException{
		jsonldData();
		updateId = id;
		System.out.println("Id getting inserted into db:"+id);
		setValidAuthToken();
		addEntity();
		checkSuccessfulResponse();
	}
	
	@Then("^error message is (.*)")
	public void verifyUnsuccessfulMessage(String message) throws JsonParseException, JsonMappingException, IOException{
		assertEquals(message, response.getBody().getParams().getErrmsg());
	}
	
	@Given("^a non existent record id$")
	public void a_non_existent_record_id() throws Exception {
		id = generateRandomId();
	}
	
	@When("^the auth token is invalid")
	public void auth_token_is_invalid(){
		setInvalidAuthToken();
	}
	
	@When("^the auth token is missing")
	public void auth_token_is_missing(){
		headers = new HttpHeaders();
	}

	@When("^retrieving the record from the registry$")
	public void retrieving_the_record_from_the_registry(){
		response = callRegistryReadAPI();
	}

	private ResponseEntity<Response> callRegistryReadAPI() {
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<Response> response = restTemplate.exchange(baseUrl+READ_ENTITY+"/"+id, HttpMethod.GET,entity,Response.class);
		return response;
		
	}

	@Then("^record retrieval should be unsuccessful$")
	public void record_retrieval_should_be_unsuccessful() throws Exception {
		checkUnsuccessfulResponse();
	}
	
	@Given("^an existent record id$")
	public void an_existent_record_id() throws Exception {
		jsonldData();
		setValidAuthToken();
		addEntity();
		checkSuccessfulResponse();
	//	assertNotNull(jsonld);
	}

	@Then("^record retrieval should be successful$")
	public void record_retrieval_should_be_successful() throws Exception {
		checkSuccessfulResponse();
	}

	@Then("^the record should match$")
	public void the_record_should_match() throws Exception {
		checkForIsomorphicModel();
	}
	
	@Given("^a response")
	public void validResponseFormat(){
		try {
			setJsonld(VALID_NEWJSONLD);
			id=setJsonldWithNewRootLabel();	
			setValidAuthHeader();
			response = callRegistryCreateAPI();
		} catch (Exception e) {
			response = null;
		}
		assertNotNull(response);
	}
	
	@Given("^a read response")
	public void validReadResponseFormat(){
		try {
			id= generateRandomId();
			setValidAuthHeader();
			response=callRegistryReadAPI();
		} catch (Exception e) {
			response = null;
		}
		assertNotNull(response);
	}
	
	@When("^response matches expected format")
	public void response_format_check(){
	
		assertEquals( true, (response.getBody().getId()!=null 
				&& response.getBody().getEts() !=null 
				&& response.getBody().getVer()!=null
				&& response.getBody().getResponseCode().equalsIgnoreCase("OK")
				&& response.getBody().getParams().getErr()!=null
				&& response.getBody().getParams().getErrmsg()!=null
				&& response.getBody().getParams().getMsgid()!=null
				&& response.getBody().getParams().getResmsgid()!=null
				&& response.getBody().getParams().getStatus()!=null
				&&(response.getBody().getParams().getStatus().equals(Response.Status.SUCCCESSFUL)?
						(response.getBody().getResult()!= null) : response.getBody().getResult()==null))
	            && (response.getBody().getClass().getDeclaredFields().length == 6));
	}

	@Then("^the response format should be successful") 
	public void response_successful() throws Exception {
		assertEquals(true,response !=null);
	}	
}
