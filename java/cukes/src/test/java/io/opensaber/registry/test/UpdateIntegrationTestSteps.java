package io.opensaber.registry.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import cucumber.api.Scenario;
import cucumber.api.java8.En;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.Response.Status;
import io.opensaber.pojos.ResponseParams;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class UpdateIntegrationTestSteps extends RegistryTestBase implements En {

	private static final String VALID_JSONLD_FILE = "create_teacher.jsonld";
	private static final String INVALID_UPDATE_JSONLD = "invalid-teacher.jsonld";
	private static final String CREATE_REST_ENDPOINT = "add";
	private static final String UPDATE_REST_ENDPOINT = "update";
	private static final String READ_REST_ENDPOINT = "read";
	private static final String AUDIT_REST_ENDPOINT = "fetchAudit";
	Type type = new TypeToken<Map<String, String>>() {
	}.getType();
	private String baseUrl;
	private ResponseEntity<Response> response, auditBeforeUpdate, auditAfterUpdate;
	private String id;
	private HttpHeaders headers;

	/**
	 * The list of integration test scenarios that will be run as part of the update
	 * feature integration test
	 */
	public UpdateIntegrationTestSteps() {
		initialize();
		initializeCommonSteps();
		backgroundStepsForEachTest();
		testUpdateWithInvalidData();
		testUpdateWhenEntityDoesNotExist();
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

		When("^updating the record in the registry$", () -> {
			StringBuilder url = new StringBuilder();
			// url.append(baseUrl).append(UPDATE_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
			url.append(baseUrl).append(UPDATE_REST_ENDPOINT);
			response = update(jsonld, url.toString(), headers);
		});

		Then("^updating the record should be unsuccessful$", () -> checkUnsuccessfulResponse());

		And("^update api error message is (.*)$", (String errorMsg) -> verifyUnsuccessfulMessage(errorMsg));

		And("^validation error message is (.*)$",
				(String validationError) -> verifyValidationErrorMessage(validationError));

		Given("^valid data for updating a record$", () -> {
			StringBuilder url = new StringBuilder();
			url.append(baseUrl).append(READ_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
			// url.append(id);
			response = fetchEntity(url.toString(), headers);
			jsonld = updateInputJsonldRootNodeId(response, "update_teacher.jsonld");
		});

		Given("^input for updating single record$", () -> {
			StringBuilder url = new StringBuilder();
			url.append(baseUrl).append(READ_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
			// url.append(id);
			response = fetchEntity(url.toString(), headers);
			jsonld = updateInputJsonldRootNodeId(response, "update_teacher_audit.jsonld");
		});

		And("^audit record before update$", () -> {
			StringBuilder url = new StringBuilder();
			url.append(baseUrl).append(AUDIT_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
			auditBeforeUpdate = fetchEntity(url.toString(), headers);
		});

		Then("^updating the record should be successful", () -> checkSuccessfulResponse());

		And("^getting audit records after update$", () -> {
			StringBuilder url = new StringBuilder();
			url.append(baseUrl).append(AUDIT_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
			auditAfterUpdate = fetchEntity(url.toString(), headers);
		});

		Then("^check audit records are matched with expected records$", () -> {

			Model modelAudit = ModelFactory.createDefaultModel();
			StringReader reader = new StringReader(new Gson().toJson(auditBeforeUpdate.getBody().getResult()));
			modelAudit.read(reader, null, "JSON-LD");

			Model modelInput = ModelFactory.createDefaultModel();
			StringReader readerInput = new StringReader(new Gson().toJson(auditAfterUpdate.getBody().getResult()));
			modelInput.read(readerInput, null, "JSON-LD");
			Model diff = modelInput.difference(modelAudit);

			StmtIterator sIter = diff.listStatements();
			while (sIter.hasNext()) {
				Statement stmt = sIter.nextStatement();
				if (stmt.getPredicate().toString().contains("newObject")) {
					assertEquals(true, stmt.getObject().toString().contains("14"));
				}
			}
		});

		Then("^check audit is disabled$", () -> {

			Status responseStatus = auditBeforeUpdate.getBody().getParams().getStatus();
			assertEquals(Response.Status.UNSUCCESSFUL, responseStatus);

			ResponseParams responseParams = auditBeforeUpdate.getBody().getParams();
			assertEquals(responseParams.getErrmsg(), "Audit is disabled");

			responseStatus = auditAfterUpdate.getBody().getParams().getStatus();
			assertEquals(Response.Status.UNSUCCESSFUL, responseStatus);

			responseParams = auditAfterUpdate.getBody().getParams();
			assertEquals(responseParams.getErrmsg(), "Audit is disabled");
		});
	}

	/**
	 * Cucumber Background steps which will be executed before each test
	 */
	private void backgroundStepsForEachTest() {
		Given("a valid entity record$", () -> {
			setJsonld(VALID_JSONLD_FILE);
		});
		And("^the record is created in the registry$", () -> {
			StringBuilder url = new StringBuilder();
			url.append(baseUrl).append(CREATE_REST_ENDPOINT);
			ResponseEntity<Response> response = createEntity(jsonld, url.toString(), headers);
			id = (String) ((Map) response.getBody().getResult()).get("entity");
		});
	}

	private String updateInputJsonldRootNodeId(ResponseEntity<Response> response, String updateJsonFilename) {

		Map<String, Object> responseData = (Map) response.getBody().getResult();
		Gson gson = new Gson();
		JsonParser parser = new JsonParser();
		JsonObject responseResult = parser.parse(gson.toJson(responseData)).getAsJsonObject();
		JsonObject entity = responseResult.getAsJsonArray("@graph").get(0).getAsJsonObject();
		String entityId = entity.get("@id").getAsString();

		JsonObject updateJsonObject = parser
				.parse(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(updateJsonFilename)))
				.getAsJsonObject();
		updateJsonObject.getAsJsonObject("request").addProperty("@id", entityId);
		return gson.toJson(updateJsonObject);
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

	private void verifyValidationErrorMessage(String message) {
		Map<String, Object> result = (Map) response.getBody().getResult();
		Map<String, String> validationErrorData = (Map<String, String>) result.get("data");
		String validationError = validationErrorData.get(Constants.INTEGRATION_TEST_BASE_URL + "serialNum");
		assertEquals(message, validationError);
	}

	private void testUpdateWithInvalidData() {
		After((Scenario scenario) -> response = null);
		Given("^an invalid data for updating a record$", () -> {
			setJsonld(INVALID_UPDATE_JSONLD);
			setJsonldWithNewRootLabel(extractIdWithoutContext(id));
		});
	}

	private void testUpdateWhenEntityDoesNotExist() {
		After((Scenario scenario) -> response = null);

		Given("^a non-existent record id$", () -> {
			id = generateRandomId();
			setJsonldWithNewRootLabel(id);
		});
	}

}
