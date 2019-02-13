package io.opensaber.registry.test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CharStreams;
import com.google.gson.reflect.TypeToken;
import cucumber.api.java.en.Then;
import cucumber.api.java8.En;
import io.opensaber.pojos.Response;
import io.opensaber.registry.transform.JsonldToJsonTransformer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SearchIntegrationTestSteps extends RegistryTestBase implements En {

	private static final String CREATE_REST_ENDPOINT = "add";
	private static final String SEARCH_REST_ENDPOINT = "search";
	private static final String VALID_JSONLD_FILE = "create_teacher.jsonld";
	private static ObjectMapper mapper = new ObjectMapper();
	Type type = new TypeToken<Map<String, String>>() {
	}.getType();
	private String baseUrl;
	private ResponseEntity<Response> response;
	private HttpHeaders headers;
	private ObjectNode baseJson;

	public SearchIntegrationTestSteps() throws IOException {
		initialize();
		initializeCommonSteps();
		backgroundStepsForEachTest();
	}

	public void initialize() throws IOException {
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(requestFactory);
		baseUrl = generateBaseUrl();
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("x-authenticated-user-token", accessToken);
	}

	/**
	 * Cucumber Background steps which will be executed before each test
	 */
	private void backgroundStepsForEachTest() {
		Given("a record to add$", () -> {
			setJsonld(VALID_JSONLD_FILE);
		});
		And("^adding record in the registry$", () -> {
			StringBuilder url = new StringBuilder();
			url.append(baseUrl).append(CREATE_REST_ENDPOINT);
			ResponseEntity<Response> response = createEntity(jsonld, url.toString(), headers);
			id = (String) ((Map) response.getBody().getResult()).get("entity");
		});
	}

	private void initializeCommonSteps() {
		When("^searching record in the registry$", () -> {
			StringBuilder url = new StringBuilder();
			url.append(baseUrl).append(SEARCH_REST_ENDPOINT);
			response = search(baseJson.toString(), url.toString(), headers);
		});

		Then("^search api message is successful", () -> checkSuccessfulResponse());

		Then("^search api message is unsuccessful", () -> checkUnsuccessfulResponse());

		And("^response must have atleast one record$", () -> {
			Map<String, Object> result = (Map) response.getBody().getResult();
			JSONObject obj = new JSONObject(result);
			JSONArray resultArray = (JSONArray) obj.get("@graph");
			assertTrue(resultArray.length() >= 1);
		});
	}

	@cucumber.api.java.en.Given("^a search filter with no match in the registry$")
	public void searchFilterWithNoMatch() throws IOException {
		baseJson = (ObjectNode) mapper.readTree(CharStreams.toString(new InputStreamReader(
				JsonldToJsonTransformer.class.getClassLoader().getResourceAsStream("base_search_context.jsonld"))));
		ObjectNode node = (ObjectNode) baseJson.get("request");
		node.put("serialNum", 13);
	}

	@cucumber.api.java.en.Given("^a search filter with single property$")
	public void searchFilterForSingleProperty() throws IOException {
		baseJson = (ObjectNode) mapper.readTree(CharStreams.toString(new InputStreamReader(
				JsonldToJsonTransformer.class.getClassLoader().getResourceAsStream("base_search_context.jsonld"))));
		ObjectNode node = (ObjectNode) baseJson.get("request");
		node.put("serialNum", 12);
	}

	@cucumber.api.java.en.Given("^a search filter with multiple properties$")
	public void searchFilterForMultipleProperty() throws IOException {
		baseJson = (ObjectNode) mapper.readTree(CharStreams.toString(new InputStreamReader(
				JsonldToJsonTransformer.class.getClassLoader().getResourceAsStream("base_search_context.jsonld"))));
		ObjectNode node = (ObjectNode) baseJson.get("request");
		node.put("serialNum", 12);
		node.put("highestAcademicQualification", "AcademicQualificationTypeCode-PHD");
	}

	@cucumber.api.java.en.Given("^a search filter with multiple values for a property$")
	public void searchFilterForPropertyWithMultipleValues() throws IOException {
		baseJson = (ObjectNode) mapper.readTree(CharStreams.toString(new InputStreamReader(
				JsonldToJsonTransformer.class.getClassLoader().getResourceAsStream("base_search_context.jsonld"))));
		List<String> list = new ArrayList<String>();
		list.add("AcademicQualificationTypeCode-PHD");
		list.add("AcademicQualificationTypeCode-POSTDOC");
		JsonNode node = mapper.valueToTree(list);
		ObjectNode objNode = (ObjectNode) baseJson.get("request");
		objNode.put("highestAcademicQualification", node);
	}

	@cucumber.api.java.en.Given("^a search filter without entity type$")
	public void searchFilterWithoutEntityType() throws IOException {
		baseJson = (ObjectNode) mapper.readTree(CharStreams.toString(new InputStreamReader(
				JsonldToJsonTransformer.class.getClassLoader().getResourceAsStream("base_search_context.jsonld"))));
		ObjectNode node = (ObjectNode) baseJson.get("request");
		node.put("serialNum", 12);
		node.remove("@type");
	}

	@cucumber.api.java.en.And("^result is empty$")
	public void responseWithEmptyResult() {
		Map<String, Object> result = (Map) response.getBody().getResult();
		JSONObject obj = new JSONObject(result);
		assertFalse(obj.has("@graph"));
	};

	@Then("^message is (.*)")
	public void verifyUnsuccessfulMessage(String message) throws JsonParseException, JsonMappingException, IOException {
		assertEquals(message, response.getBody().getParams().getErrmsg());
	}

	private void checkSuccessfulResponse() {
		Response.Status responseStatus = response.getBody().getParams().getStatus();
		assertEquals(Response.Status.SUCCESSFUL, responseStatus);
	}

	private void checkUnsuccessfulResponse() {
		Response.Status responseStatus = response.getBody().getParams().getStatus();
		assertEquals(Response.Status.UNSUCCESSFUL, responseStatus);
	}

}
