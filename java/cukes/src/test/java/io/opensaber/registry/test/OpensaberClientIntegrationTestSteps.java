package io.opensaber.registry.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CharStreams;
import cucumber.api.java8.En;
import io.opensaber.pojos.Response;
import io.opensaber.registry.client.OpensaberClient;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.exception.NodeMappingNotDefinedException;
import io.opensaber.registry.exception.TransformationException;
import io.opensaber.registry.transform.ITransformer;
import io.opensaber.registry.transform.JsonToJsonLDTransformer;
import io.opensaber.registry.transform.JsonldToJsonTransformer;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class OpensaberClientIntegrationTestSteps extends RegistryTestBase implements En {

    private OpensaberClient client;

    private JsonNode jsonInput;
    private ResponseData<String> responseData;
    private Response response;
    private ClientExceptions clientExceptions;
    private URI entityId;

    private Map<String, String> headers = new HashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();

    public OpensaberClientIntegrationTestSteps() throws IOException {
        headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        headers.put("x-authenticated-user-token", accessToken);
        initialize();
        initializeCommonSteps();
    }

    private void initialize() {
        ITransformer<String> jsonToJsonldTransformer = JsonToJsonLDTransformer.getInstance();
        ITransformer<String> jsonldToJsonTransformer = JsonldToJsonTransformer.getInstance();
        client = OpensaberClient.builder()
                .requestTransformer(jsonToJsonldTransformer)
                .responseTransformer(jsonldToJsonTransformer).build();
        clientExceptions = new ClientExceptions();
    }

    /**
     * All the reusable step definitions should go here
     */
    private void initializeCommonSteps() {
        Given("^a valid json input for a new entity$", () -> {
            jsonInput = mapper.readTree(CharStreams.toString(new InputStreamReader(
                    JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_json_client_lib.json"))));
        });
        When("^creating the entity in the registry", () -> {
            try {
                responseData = client.addEntity(new RequestData<>(jsonInput.toString()), headers);
            } catch (TransformationException ex) {
                clientExceptions.expectException();
                clientExceptions.add(ex);
            }
        });
        Then("^response from the api should be successful", () -> checkSuccessfulResponse());


        Given("^a json input with missing field mapping$", () -> {
            jsonInput = mapper.readTree(CharStreams.toString(new InputStreamReader(
                    JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_json_client_lib.json"))));
            ObjectNode objectNode = (ObjectNode) jsonInput.path("teacher");
            objectNode.remove("yearOfJoiningService");
            objectNode.put("fieldWithMissingMapping", "2014");
        });

        Then("^creation of new entity should be unsuccessful", () -> checkNodeMappingNotDefinedExcpetion());

        And("^an existing entity in the registry$", () ->
                responseData = client.addEntity(new RequestData<>(jsonInput.toString()), headers));

        When("^updating the entity in the registry$", () -> {
            entityId = new URI(mapper.readTree(responseData.getResponseData()).path("result").path("entity").asText());
            responseData = readEntity();
            JsonNode teacherResponseNode = mapper.readTree(responseData.getResponseData()).path("result").path("teacher");
            String teachingRoleId = extractNodeId(teacherResponseNode, "teachingRole");
            ObjectNode updateNode = ((ObjectNode) mapper.readTree("{\"teacher\": {\"teachingRole\": {\"mainSubjectsTaught\": [\"SubjectCode-ENGLISH\",\"SubjectCode-MATH\"]}}}"));
            ((ObjectNode) updateNode.path("teacher")).put("id", entityId.toString());
            ((ObjectNode) updateNode.path("teacher").path("teachingRole")).put("id", teachingRoleId);
            responseData = client.updateEntity(new RequestData<>(updateNode.toString()), headers);
        });

        And("^delete the entity in the registry$" ,() -> {
            entityId = new URI(mapper.readTree(responseData.getResponseData()).path("result").path("entity").asText());
            responseData = deleteEntity();
        });
    }

    private ResponseData<String> deleteEntity() {
        return client.deleteEntity(entityId, headers);
    }

    private ResponseData<String> readEntity() throws TransformationException {
        return client.readEntity(entityId, headers);
    }

    private String extractNodeId(JsonNode response, String field) throws IOException {
        return response.path(field).path("id").asText();
    }

    private void checkSuccessfulResponse() throws IOException {
        response = mapper.readValue(responseData.getResponseData(), Response.class);
        assertEquals(Response.Status.SUCCCESSFUL, response.getParams().getStatus());
    }

    private void checkNodeMappingNotDefinedExcpetion() {
        assertEquals(clientExceptions.isExceptException(), true);
    }

    private void checkUnsuccessfulResponse() throws IOException {
        response = mapper.readValue(responseData.getResponseData(), Response.class);
        assertEquals(Response.Status.UNSUCCESSFUL, response.getParams().getStatus());
    }

}
