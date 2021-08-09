package io.opensaber.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.opensaber.workflow.StateContext;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class StateContextTest {
    private static final ObjectMapper m = new ObjectMapper();
    private static final String TEST_FOLDER = "src/test/resources/StateContext/";
    JsonNode existingNode;
    JsonNode updatedNode;

    @Before
    public void setUp() throws IOException {
        existingNode = m.readTree(new File(TEST_FOLDER + "existingNode.json"));
        updatedNode = m.readTree(new File(TEST_FOLDER + "updatedNode.json"));
    }

    @Test
    public void shouldReturnTrueIfThereIsChangeInTheUpdatedNode() throws Exception {

        StateContext stateContext = StateContext.builder()
                .existing(existingNode.at("/Student/identityDetails"))
                .updated(updatedNode.at("/Student/identityDetails"))
                .build();
        assertTrue(stateContext.isModified());
    }

    @Test
    public void shouldReturnFalseIfThereIsNoChangeInRelevantFieldsUpdatedNode() throws Exception {
        StateContext stateContext = StateContext.builder()
                .existing(existingNode.at("/Student/educationDetails/0"))
                .updated(updatedNode.at("/Student/educationDetails/1"))
                .ignoredFields(Arrays.asList("awards", "courses"))
                .build();
        assertFalse(stateContext.isModified());
    }

    @Test
    public void shouldReturnTrueIfFieldIsPresentOnlyInUpdatedNode() throws Exception {
         StateContext stateContext = StateContext.builder()
                 .existing(JsonNodeFactory.instance.objectNode())
                 .updated(updatedNode.at("/Student/educationDetails/1/awards/0"))
                 .build();
        assertTrue(stateContext.isModified());
    }
}
