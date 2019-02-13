package io.opensaber.registry.test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.Response.Status;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RegistryIntegrationSteps extends RegistryTestBase {

	// private static final String VALID_NEWJSONLD= "newSchool.jsonld";
	private static final String VALID_NEWJSONLD = "teacher.jsonld";
	private static final String ENTITY_JSONLD = "basicProficiencyLevel.jsonld";
	private static final String INVALID_LABEL_JSONLD = "invalid-label.jsonld";
	private static final String INVALID_REQUESTID_JSONLD = "invalid_request_id_teacher.jsonld";
	private static final String INVALID_NEWJSONLD = "invalid-teacher.jsonld";
	private static final String ADD_ENTITY = "add";
	private static final String READ_ENTITY = "read";
	private static final String AUTH_HEADER_NAME = "x-authenticated-user-token";
	Type type = new TypeToken<Map<String, String>>() {
	}.getType();
	private RestTemplate restTemplate;
	private String baseUrl;
	private ResponseEntity<Response> response;
	private String id;
	private HttpHeaders headers;
	private String updateId;

	@Before
	public void initializeData() {
		restTemplate = new RestTemplate();
		baseUrl = generateBaseUrl();
	}

	@Given("^a valid record")
	public void jsonldData() {
		setJsonld(VALID_NEWJSONLD);
		id = setJsonldWithNewRootLabel();
		assertNotNull(jsonld);
	}

	@Given("^an id for a non-existent record")
	public void non_existent_record() {
		id = generateRandomId();
		updateId = id;
	}

	@Given("^a record with invalid type")
	public void invalidTypeJsonldData() {
		setJsonld(INVALID_LABEL_JSONLD);
		id = setJsonldWithNewRootLabel();
		assertNotNull(jsonld);
	}

	@Given("^an invalid request id for record")
	public void invalidRequestIdOfJsonldData() {
		setJsonld(INVALID_REQUESTID_JSONLD);
		id = setJsonldWithNewRootLabel();
		assertNotNull(jsonld);
	}

	@And("^an invalid record")
	public void invalidJsonldData() {
		setJsonld(INVALID_NEWJSONLD);
		id = setJsonldWithNewRootLabel();
		assertNotNull(jsonld);
	}

	@And("^a valid auth token")
	public void setValidAuthToken() {
		setValidAuthHeader();
		assertNotNull(headers);
	}

	@And("^an invalid auth token")
	public void setInvalidAuthToken() {
		setInvalidAuthHeader();
		assertNotNull(headers);
	}

	@And("^a missing auth token")
	public void setMissingAuthToken() {
		headers = new HttpHeaders();
		assertNotNull(headers);
	}

	@When("^issuing the record into the registry")
	public void addEntity() {
		response = callRegistryCreateAPI();
	}

	@When("^an entity for the record is issued into the registry$")
	public void add_entity_to_existing_record_in_registry() {
		jsonldData(ENTITY_JSONLD);
		response = callRegistryCreateAPI(baseUrl + updateId, baseUrl + "basicProficiencyLevel");
	}

	@When("^the same entity for the record is issued into the registry$")
	public void add_existing_entity_to_existing_record_in_registry() {
		response = callRegistryCreateAPI(baseUrl + updateId, baseUrl + "basicProficiencyLevel");
	}

	public void jsonldData(String filename) {
		setJsonld(filename);
		id = setJsonldWithNewRootLabel();
		assertNotNull(jsonld);
	}

	private ResponseEntity<Response> callRegistryCreateAPI() {
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonld, headers);
		ResponseEntity<Response> response = restTemplate.postForEntity(baseUrl + ADD_ENTITY, entity, Response.class);
		return response;
	}

	private ResponseEntity<Response> callRegistryCreateAPI(String entityLabel, String property) {
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonld, headers);
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("id", entityLabel);
		uriVariables.put("prop", property);
		ResponseEntity<Response> response = restTemplate.postForEntity(baseUrl + ADD_ENTITY + "?id={id}&prop={prop}",
				entity, Response.class, uriVariables);
		return response;
	}

	@Then("^record issuing should be successful")
	public void verifySuccessfulResponse() throws JsonParseException, JsonMappingException, IOException {
		checkSuccessfulResponse();
	}

	@Then("^record issuing should be unsuccessful")
	public void verifyUnsuccessfulResponse() throws JsonParseException, JsonMappingException, IOException {
		checkUnsuccessfulResponse();
	}

	@And("^fetching the record from the registry should match the issued record")
	public void fetchRecordFromRegistryAndVerify() throws JsonParseException, JsonMappingException, IOException {
		setValidAuthToken();
		response = callRegistryReadAPI();
		checkForIsomorphicModel();
	}

	private void checkSuccessfulResponse() throws JsonParseException, JsonMappingException, IOException {
		Status responseStatus = response.getBody().getParams().getStatus();
		assertEquals(Response.Status.SUCCESSFUL, responseStatus);
	}

	private void checkUnsuccessfulResponse() throws JsonParseException, JsonMappingException, IOException {
		Status responseStatus = response.getBody().getParams().getStatus();
		assertEquals(Response.Status.UNSUCCESSFUL, responseStatus);
	}

	private void checkForIsomorphicModel() throws IOException {
		Model expectedModel = ModelFactory.createDefaultModel();
		ObjectMapper mapper = new ObjectMapper();
		String jsonldBody = mapper.readTree(jsonld).path("request").toString();
		RDFDataMgr.read(expectedModel, new StringReader(jsonldBody), null, org.apache.jena.riot.RDFLanguages.JSONLD);
		Map<String, Object> result = (Map) response.getBody().getResult();
		Model actualModel = ModelFactory.createDefaultModel();
		String newJsonld = new JSONObject(result).toString(2);
		RDFDataMgr.read(actualModel, new StringReader(newJsonld), null, org.apache.jena.riot.RDFLanguages.JSONLD);
		assertTrue(expectedModel.isIsomorphicWith(actualModel));
	}

	private void setValidAuthHeader() {
		headers = new HttpHeaders();
		headers.add(AUTH_HEADER_NAME, accessToken);
	}

	public HttpHeaders getHeaders() {

		return headers;
	}

	private void setInvalidAuthHeader() {
		headers = new HttpHeaders();
		headers.add(AUTH_HEADER_NAME, "1234");
	}

	@Given("(.*) record issued into the registry")
	public void issueRecordInRegistry(String qualifier) throws JsonParseException, JsonMappingException, IOException {
		jsonldData();
		updateId = id;
		setValidAuthToken();
		addEntity();
		checkSuccessfulResponse();
	}

	@Then("^error message is (.*)")
	public void verifyUnsuccessfulMessage(String message) throws JsonParseException, JsonMappingException, IOException {
		assertEquals(message, response.getBody().getParams().getErrmsg());
	}

	@Given("^a non existent record id$")
	public void a_non_existent_record_id() throws Exception {
		id = generateRandomId();
	}

	@When("^the auth token is invalid")
	public void auth_token_is_invalid() {
		setInvalidAuthToken();
	}

	@When("^the auth token is missing")
	public void auth_token_is_missing() {
		headers = new HttpHeaders();
	}

	@When("^retrieving the record from the registry$")
	public void retrieving_the_record_from_the_registry() {
		response = callRegistryReadAPI();
	}

	private ResponseEntity<Response> callRegistryReadAPI() {
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<Response> response = restTemplate.exchange(baseUrl + READ_ENTITY + "/" + id, HttpMethod.GET,
				entity, Response.class);
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
		// assertNotNull(jsonld);
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
	public void validResponseFormat() {
		try {
			setJsonld(VALID_NEWJSONLD);
			id = setJsonldWithNewRootLabel();
			setValidAuthHeader();
			response = callRegistryCreateAPI();
		} catch (Exception e) {
			response = null;
		}
		assertNotNull(response);
	}

	@Given("^a read response")
	public void validReadResponseFormat() {
		try {
			id = generateRandomId();
			setValidAuthHeader();
			response = callRegistryReadAPI();
		} catch (Exception e) {
			response = null;
		}
		assertNotNull(response);
	}

	@When("^response matches expected format")
	public void response_format_check() {

		assertEquals(true, (response.getBody().getId() != null && response.getBody().getEts() != null
				&& response.getBody().getVer() != null && response.getBody().getResponseCode().equalsIgnoreCase("OK")
				&& response.getBody().getParams().getErr() != null && response.getBody().getParams().getErrmsg() != null
				&& response.getBody().getParams().getMsgid() != null
				&& response.getBody().getParams().getResmsgid() != null
				&& response.getBody().getParams().getStatus() != null
				&& (response.getBody().getParams().getStatus().equals(Response.Status.SUCCESSFUL)
						? (response.getBody().getResult() != null)
						: response.getBody().getResult() == null))
				&& (response.getBody().getClass().getDeclaredFields().length == 6));
	}

	@Then("^the response format should be successful")
	public void response_successful() throws Exception {
		assertEquals(true, response != null);
	}

	@Then("^record should never have any associated audit info$")
	public void test_audit_record_unexpected_in_read() throws Exception {
		response = callRegistryReadAPI();
		Map<String, Object> map = (Map) response.getBody().getResult();
		if (map.containsKey("(?i)(?<= |^)audit(?= |$)")) {
			checkUnsuccessfulResponse();
		} else {
			checkSuccessfulResponse();
		}
	}
}
