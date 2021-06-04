package io.opensaber.registry.model.state;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.middleware.util.JSONUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.*;

public class StateContextTest {
    String currentRole;
    JsonNode requestBody;

    @Before
    public void setUp() throws IOException {
        currentRole = "student";
        requestBody = JSONUtil.convertStringJsonNode("{\n" +
                "    \"fromYear\": \"1999\",\n" +
                "    \"toYear\": \"2019\",\n" +
                "    \"instituteName\": \"Park Angels school\"\n" +
                "}");
    }

    @Test
    public void shouldReturnTrueIfThereIsChangeInTheUpdateRequest() throws IOException {
        JsonNode existingNode = JSONUtil.convertStringJsonNode("{\n" +
                "    \"fromYear\": \"1999\",\n" +
                "    \"toYear\": \"2019\",\n" +
                "    \"instituteName\": \"AHSS\"\n" +
                "}");
        StateContext stateContext = new StateContext(existingNode, requestBody, currentRole);
        assertTrue(stateContext.isAttributesChanged());
    }
    @Test
    public void shouldReturnTrueIfThereIsChangeInTheUpdateRequestAndEvenThoughSomeFieldsAreMissingInIt()
            throws IOException {
        JsonNode existingNode = JSONUtil.convertStringJsonNode("{\n" +
                "    \"instituteName\": \"AHSS\"\n" +
                "}");
        StateContext stateContext = new StateContext(existingNode, requestBody, currentRole);
        assertTrue(stateContext.isAttributesChanged());
    }

    @Test
    public void shouldReturnFalseIfThereIsNoChangeInTheUpdateRequest() {
        StateContext stateContext = new StateContext(requestBody, requestBody, currentRole);
        assertFalse(stateContext.isAttributesChanged());
    }

    @Test
    public void shouldReturnFalseIfThereIsNoChangeInTheUpdateRequestAndEvenThoughSomeFieldsAreMissingInIt()
            throws IOException {
        JsonNode existingNode = JSONUtil.convertStringJsonNode("{\n" +
                "    \"fromYear\": \"1999\"\n" +
                "}");
        StateContext stateContext = new StateContext(existingNode, requestBody, currentRole);
        assertFalse(stateContext.isAttributesChanged());
    }

    @Test
    public void shouldCopyTheRequestBodyToTheResultInitially() {
        StateContext stateContext = new StateContext(currentRole, requestBody);
        assertEquals(requestBody, stateContext.getResult());
    }

    @Test
    public void shouldSetTheGivenStateToTheResult() throws IOException {
        StateContext stateContext = new StateContext(currentRole, requestBody);
        for (States state : States.values()) {
            stateContext.setState(state);
            JsonNode expectedResult = JSONUtil.convertStringJsonNode("{\n" +
                    "    \"fromYear\": \"1999\",\n" +
                    "    \"toYear\": \"2019\",\n" +
                    "    \"instituteName\": \"Park Angels school\",\n" +
                    "    \"_osState\": " + "\"" + state.toString() + "\"" + "\n" +
                    "}\n");
            assertEquals(expectedResult, stateContext.getResult());
        }
    }

    @Test
    public void shouldNotSetTheSendAttributeToTheResult() throws IOException {
        JsonNode existingNode = JSONUtil.convertStringJsonNode("{\n" +
                "    \"fromYear\": \"1999\",\n" +
                "    \"toYear\": \"2019\",\n" +
                "    \"instituteName\": \"AHSS\"\n" +
                "}");
        JsonNode requestBody = JSONUtil.convertStringJsonNode("{\n" +
                "    \"fromYear\": \"1999\",\n" +
                "    \"send\": \"true\"\n" +
                "}");
        StateContext stateContext = new StateContext(existingNode, requestBody, currentRole);
        assertFalse(stateContext.getResult().has("send"));
    }
}
