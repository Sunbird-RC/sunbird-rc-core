package io.opensaber.registry.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cucumber.api.Scenario;
import cucumber.api.java8.En;
import io.opensaber.pojos.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UpdateIntegrationTestSteps extends RegistryTestBase implements En {

    private static final String VALID_JSONLD_FILE = "create_teacher.jsonld";
    private static final String VALID_UPDATE_JSONLD_FILE = "update_teacher.jsonld";
    private static final String INVALID_UPDATE_JSONLD = "invalid-teacher.jsonld";
    private static final String CREATE_REST_ENDPOINT = "create";
    private static final String UPDATE_REST_ENDPOINT = "update";
    private static final String READ_REST_ENDPOINT = "read";
    private static final String CONTEXT_CONSTANT = "sample:";
    private static final String AUDIT_REST_ENDPOINT = "fetchAudit";

    // private RestTemplate restTemplate;
    private String baseUrl;
    private ResponseEntity<Response> response;
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

        Then("^updating the record should be successful", () -> checkSuccessfulResponse());
        
        And("^getting audit records for update$", () -> {          
        	 StringBuilder url = new StringBuilder();
             url.append(baseUrl).append(AUDIT_REST_ENDPOINT).append("/").append(extractIdWithoutContext(id));
             response = fetchEntity(url.toString(),headers);          	
        });
        
        Then("^check audit records are matched with expected records$",() -> {
        	
        	 Object auditRecords = response.getBody().getResult().get("@graph");
          	             
             Map<String,List<Object>> updatedProperties=new HashMap<>();
             List<Object> values1= new ArrayList<>();
             values1.add("12");
             values1.add("12");
             updatedProperties.put("serialNum", values1);
             List<Object> values2= new ArrayList<>();
             values2.add("12234");
             values2.add("17382");
             updatedProperties.put("teacherCode", values2);
             List<Object> values3= new ArrayList<>();
             values3.add("Marvin Pande");
             values3.add("Akshaya Vijayalakshmi");
             updatedProperties.put("teacherName", values3);
             List<Object> values4= new ArrayList<>();
             values4.add("2014");
             values4.add("2016");
             updatedProperties.put("@value", values4);    
             
             
        	
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
