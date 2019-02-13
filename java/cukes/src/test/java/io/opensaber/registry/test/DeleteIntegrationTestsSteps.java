package io.opensaber.registry.test;

import cucumber.api.Scenario;
import cucumber.api.java8.En;
import io.opensaber.pojos.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DeleteIntegrationTestsSteps extends RegistryTestBase implements En {

	private static final String VALID_JSONLD_FILE = "create_teacher.jsonld";
	private static final String CREATE_REST_ENDPOINT = "add";
	private static final String DELETE_REST_ENDPOINT = "delete";
	private String baseUrl;
	private String id;
	private ResponseEntity<Response> response;
	private HttpHeaders headers;

	/**
	 * The list of integration test scenarios that will be run as part of the update
	 * feature integration test
	 */
	public DeleteIntegrationTestsSteps() {
		initialize();
		initializeCommonSteps();
		backgroundStepsForEachTest();
		testDeleteWithParentRecordIsActive();
		testDeleteWhenEntityDoesNotExist();
	}

	public void initialize() {
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(requestFactory);
		baseUrl = generateBaseUrl();
		headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("x-authenticated-user-token", accessToken);
	}

	/**
	 * All the reusable step definitions should go here
	 */
	private void initializeCommonSteps() {
		Given("^delete the record in the registry$", () -> {
			StringBuilder url = new StringBuilder();
			url.append(baseUrl).append(DELETE_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
			response = delete(url.toString(), headers);
		});

		Then("^deleted record should be unsuccessful$", () -> checkUnsuccessfulResponse());
		And("^delete api error message is (.*)$", (String errorMsg) -> verifyUnsuccessfulMessage(errorMsg));

		/*
		 * Given("^delete record with connected nodes are active$", () -> {
		 * StringBuilder url = new StringBuilder();
		 * url.append(baseUrl).append(DELETE_REST_ENDPOINT).append("/").append(
		 * "DisabilityCode-NA"); response = delete(url.toString(),headers); });
		 */
		Then("^deleted record should be successful$", () -> checkSuccessfulResponse());
		And("^delete api error message when parent record active is (.*)$",
				(String errorMsg) -> verifyUnsuccessfulMessageWhenParentNodeIsActive(errorMsg));

	}

	/**
	 * Cucumber Background steps which will be executed before each test
	 */
	private void backgroundStepsForEachTest() {
		Given("a valid create entity record$", () -> {
			setJsonld(VALID_JSONLD_FILE);
		});
		And("the record is inserted in the registry$", () -> {
			StringBuilder url = new StringBuilder();
			url.append(baseUrl).append(CREATE_REST_ENDPOINT);
			ResponseEntity<Response> response = createEntity(jsonld, url.toString(), headers);
			id = (String) ((Map) response.getBody().getResult()).get("entity");
		});
	}

	private void checkUnsuccessfulResponse() {
		Response.Status responseStatus = response.getBody().getParams().getStatus();
		assertEquals(Response.Status.UNSUCCESSFUL, responseStatus);
	}

	private void checkSuccessfulResponse() {
		Response.Status responseStatus = response.getBody().getParams().getStatus();
		assertEquals(Response.Status.SUCCESSFUL, responseStatus);
	}

	private void verifyUnsuccessfulMessage(String message) {
		assertEquals(message, response.getBody().getParams().getErrmsg());
	}

	private void verifyUnsuccessfulMessageWhenParentNodeIsActive(String message) {
		assertEquals(message, response.getBody().getParams().getErrmsg());
	}

	private void testDeleteWithParentRecordIsActive() {
		After((Scenario scenario) -> response = null);
		Given("^delete record with connected nodes are active$", () -> {
			id = "DisabilityCode-NA";
		});
	}

	private void testDeleteWhenEntityDoesNotExist() {
		After((Scenario scenario) -> response = null);
		Given("^delete a non-existent record id$", () -> {
			id = generateRandomId();
		});
	}

}