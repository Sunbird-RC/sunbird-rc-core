package dev.sunbirdrc.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateContextTest {
    private static final ObjectMapper m = new ObjectMapper();
    private static final String TEST_FOLDER = "src/test/resources/StateContext/";
    JsonNode existingNode;
    JsonNode updatedNode;

    @BeforeEach
    void setUp() throws IOException {
        existingNode = m.readTree(new File(TEST_FOLDER + "existingNode.json"));
        updatedNode = m.readTree(new File(TEST_FOLDER + "updatedNode.json"));
    }

    @Test
    void shouldReturnTrueIfThereIsChangeInTheUpdatedNode() {
        StateContext stateContext = StateContext.builder()
                .existing(existingNode.at("/Student/identityDetails"))
                .updated(updatedNode.at("/Student/identityDetails"))
                .build();
        assertTrue(stateContext.isModified());
    }

    @Test
    void shouldReturnFalseIfThereIsNoChangeInRelevantFieldsUpdatedNode() {
        StateContext stateContext = StateContext.builder()
                .existing(existingNode.at("/Student/educationDetails/0"))
                .updated(updatedNode.at("/Student/educationDetails/1"))
                .ignoredFields(Arrays.asList("awards", "courses"))
                .build();
        assertFalse(stateContext.isModified());
    }

    @Test
    void shouldReturnTrueIfFieldIsPresentOnlyInUpdatedNode() {
        StateContext stateContext = StateContext.builder()
                .existing(JsonNodeFactory.instance.objectNode())
                .updated(updatedNode.at("/Student/educationDetails/1/awards/0"))
                .build();
        assertTrue(stateContext.isModified());
    }
}