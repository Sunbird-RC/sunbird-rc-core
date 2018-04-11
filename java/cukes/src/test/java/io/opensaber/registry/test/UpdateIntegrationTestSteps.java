package io.opensaber.registry.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import cucumber.api.Scenario;
import cucumber.api.java8.En;
import io.opensaber.pojos.Response;
import org.apache.jena.rdf.model.StmtIterator;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdTripleCallback;
import com.github.jsonldjava.utils.JsonUtils;

import org.springframework.http.ResponseEntity;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class UpdateIntegrationTestSteps extends RegistryTestBase implements En {

    private static final String VALID_JSONLD_FILE = "create_teacher.jsonld";
    private static final String VALID_UPDATE_JSONLD_FILE = "update_teacher.jsonld";
    private static final String UPDATE_JSONLD__AUDIT_FILE = "update_teacher_audit.jsonld";
    private static final String INVALID_UPDATE_JSONLD = "invalid-teacher.jsonld";
    private static final String CREATE_REST_ENDPOINT = "create";
    private static final String UPDATE_REST_ENDPOINT = "update";
    private static final String READ_REST_ENDPOINT = "read";
    private static final String CONTEXT_CONSTANT = "sample:";
    private static final String AUDIT_REST_ENDPOINT = "fetchAudit";
    @Value("${registry.system.base}")
	private String registrySystemContext="http://example.com/voc/opensaber/";

    // private RestTemplate restTemplate;
    private String baseUrl;
    private ResponseEntity<Response> response,auditBeforeUpdate, auditAfterUpdate;
    private String id;
    private HttpHeaders headers;
    
     /**
     * The list of integration test scenarios that will be run as part of the update feature
     * integration test
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
        headers.add("x-authenticated-user-token", "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1WXhXdE4tZzRfMld5MG5PS1ZoaE5hU0gtM2lSSjdXU25ibFlwVVU0TFRrIn0.eyJqdGkiOiI2OTBiNDZjZS03MjI5LTQ5NjgtODU4Yy0yMzNjNmJhZjMxODMiLCJleHAiOjE1MjE1NjI0NDUsIm5iZiI6MCwiaWF0IjoxNTIxNTE5MjQ1LCJpc3MiOiJodHRwczovL3N0YWdpbmcub3Blbi1zdW5iaXJkLm9yZy9hdXRoL3JlYWxtcy9zdW5iaXJkIiwiYXVkIjoiYWRtaW4tY2xpIiwic3ViIjoiYWJkYmRjYzEtZDI5Yy00ZTQyLWI1M2EtODVjYTY4NzI3MjRiIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWRtaW4tY2xpIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiZmZiYWE2ZWUtMDhmZi00OGVlLThlYTEtZTI3YzhlZTE5ZDVjIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlc291cmNlX2FjY2VzcyI6e30sIm5hbWUiOiJSYXl1bHUgVmlsbGEiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ2cmF5dWx1IiwiZ2l2ZW5fbmFtZSI6IlJheXVsdSIsImZhbWlseV9uYW1lIjoiVmlsbGEiLCJlbWFpbCI6InJheXVsdUBnbWFpbC5jb20ifQ.U1hsUoXGYKtYssOkytMo_tnexHhwKs86IXrDw8rhL9tpG5c6DArVJvdhn5wTEbgzp52efNwQ5LrGGmpBFRWDw0szA5ggT347RCbTTxXZEFF2bUEE8rr0KbkfPOwk5Gazo_xRerW-URyWPlzqppZaUPc6kzY8TDouGmKF8qyVenaxrRgbhKNRYbZWFviARLytZTMLtgLafhmOvj6r3vK-kt36afUNROBSoNaxhcvSF9QnTRB1_0Bnb_qyVMqEDSdwZdGs3rMU_W8SFWMewxxXPuYWEXIvXIr2AMs7naCR4colLGz8AOMFR44-qTEF-eF71qqBNouh1hgd4N0l4sKzxA");
    }

    /**
     * All the reusable step definitions should go here
     */
    private void initializeCommonSteps() {

        When("^updating the record in the registry$", () -> {
            StringBuilder url = new StringBuilder();
            url.append(baseUrl).append(UPDATE_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
            response = updateEntity(jsonld, url.toString(), headers);
         });

        Then("^updating the record should be unsuccessful$", () -> checkUnsuccessfulResponse());

        And("^update api error message is (.*)$", (String errorMsg) -> verifyUnsuccessfulMessage(errorMsg));

        And("^validation error message is (.*)$", (String validationError) -> verifyValidationErrorMessage(validationError));

        Given("^valid data for updating a record$", () -> {
            StringBuilder url = new StringBuilder();
            url.append(baseUrl).append(READ_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
            response = fetchEntity(url.toString(), headers);
            jsonld = updateInputJsonldRootNodeId(response);
        });
        
        Given("^input for updating single record$", () -> {
            StringBuilder url = new StringBuilder();
            url.append(baseUrl).append(READ_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
            response = fetchEntity(url.toString(), headers);
            jsonld = updateInputJsonldRootNodeIdForAudit(response);
        });
        
        And("^audit record before update$", () -> {          
       	 StringBuilder url = new StringBuilder();
            url.append(baseUrl).append(AUDIT_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
            auditBeforeUpdate = fetchEntity(url.toString(),headers);          	
       });
        
        Then("^updating the record should be successful", () -> checkSuccessfulResponse());
        
        And("^getting audit records after update$", () -> {          
        	 StringBuilder url = new StringBuilder();
             url.append(baseUrl).append(AUDIT_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
             auditAfterUpdate = fetchEntity(url.toString(),headers);          	
        });
        
        Then("^check audit records are matched with expected records$", () -> {

			Model modelAudit = ModelFactory.createDefaultModel();
			StringReader reader = new StringReader(new Gson().toJson(auditBeforeUpdate.getBody().getResult()));
			modelAudit.read(reader, null, "JSON-LD");

			Model modelInput = ModelFactory.createDefaultModel();
			StringReader readerInput = new StringReader(new Gson().toJson(auditAfterUpdate.getBody().getResult()));
			modelInput.read(readerInput, null, "JSON-LD");
			StmtIterator itr = modelAudit.listStatements();
			Model diff = modelInput.difference(modelAudit);
			System.out.println("Model Diff: " + diff);
					
			StmtIterator sIter;
			if (diff.size() != 0) {
				sIter = diff.listStatements();
				while (sIter.hasNext()) {
					Statement stmt=sIter.nextStatement();
					if (stmt.getPredicate().toString().contains("newObject")) {
						assertEquals(true, stmt.getObject().toString().contains("FEMALE"));
					} 				
				}
			}
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
            id = (String) response.getBody().getResult().get("entity");
        });
    }

    private String updateInputJsonldRootNodeId(ResponseEntity<Response> response) {

        Map<String, Object> responseData = response.getBody().getResult();
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonObject responseResult = parser.parse(gson.toJson(responseData)).getAsJsonObject();
        JsonObject entity = responseResult.getAsJsonArray("@graph").get(0).getAsJsonObject();
        String entityId = entity.get("@id").getAsString();

        JsonObject updateJsonObject = parser.parse(new InputStreamReader
                (this.getClass().getClassLoader().getResourceAsStream("update_teacher.jsonld"))).getAsJsonObject();
        updateJsonObject.getAsJsonObject("request").addProperty("@id", entityId);
        return gson.toJson(updateJsonObject);
    }
    
    private String updateInputJsonldRootNodeIdForAudit(ResponseEntity<Response> response) {

        Map<String, Object> responseData = response.getBody().getResult();
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonObject responseResult = parser.parse(gson.toJson(responseData)).getAsJsonObject();
        JsonObject entity = responseResult.getAsJsonArray("@graph").get(0).getAsJsonObject();
        String entityId = entity.get("@id").getAsString();

        JsonObject updateJsonObject = parser.parse(new InputStreamReader
                (this.getClass().getClassLoader().getResourceAsStream("update_teacher_audit.jsonld"))).getAsJsonObject();
        updateJsonObject.getAsJsonObject("request").addProperty("@id", entityId);
        return gson.toJson(updateJsonObject);
    }

    private void checkUnsuccessfulResponse() {
        Response.Status responseStatus = response.getBody().getParams().getStatus();
        assertEquals(Response.Status.UNSUCCESSFUL, responseStatus);
    }

    private void checkSuccessfulResponse() {
        Response.Status responseStatus = response.getBody().getParams().getStatus();
        assertEquals(Response.Status.SUCCCESSFUL, responseStatus);
    }

    private void verifyUnsuccessfulMessage(String message) {
        assertEquals(message, response.getBody().getParams().getErrmsg());
    }

    private void verifyValidationErrorMessage(String message) {
        Map<String, Object> result = response.getBody().getResult();
        Map<String, String> validationErrorData = (Map<String, String>) result.get("data");
        String validationError = validationErrorData.get("http://example.com/voc/teacher/1.0.0/serialNum");
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
