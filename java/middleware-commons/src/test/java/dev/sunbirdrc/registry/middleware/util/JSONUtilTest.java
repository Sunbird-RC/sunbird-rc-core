package dev.sunbirdrc.registry.middleware.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.AuditRecord;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JSONUtilTest {
    private static final Logger logger = LoggerFactory.getLogger(JSONUtilTest.class);
    private static final String TEST_DIR = "src/test/resources/";
    private static final String ACTUAL_DATA = "actual_data.json";
    private static final String EXPECTED_DATA = "expected_data.json";
    private static final String EXPECTED_REPLACE_FIELD_DATA = "expected_replace_field_data.json";
    private static final String EXPECTED_ADDED_NODE_DATA = "expected_added_node_data.json";
    private static final String EXPECTED_ADDED_PREFIX_DATA = "expected_add_prefix_data.json";

    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode getNodeData(String fileName) {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        try {
            objectNode = (ObjectNode) mapper.readTree(getContent(fileName));
        } catch (IOException e) {
            // let the test fail with null
        }
        return objectNode;
    }

    @Test
    void replaceField() {
        ObjectNode actualNode = getNodeData(ACTUAL_DATA);
        ObjectNode expectedNode = getNodeData(EXPECTED_REPLACE_FIELD_DATA);

        JSONUtil.replaceField(actualNode, Constants.JsonldConstants.ID, "newValue");
        assertEquals(actualNode, expectedNode);
    }

    @Test
    void trimPrefix() {
        ObjectNode testDataNode = JsonNodeFactory.instance.objectNode();
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        try {
            testDataNode = (ObjectNode) mapper.readTree("{\"Teacher\":{\"osid\":\"1-09cc3c81-6180-4e74-aba9-f015bbaa95f1\",\"basicProficiencyLevel\":[{\"osid\":\"1-1a496e91-7886-44e2-abfa-1a40c3337d1e\"}],\"childObj\":{\"osid\":\"1-4a497b91-7886-44e2-abfa-1a40c3337d1f\"}}}");
            expected = (ObjectNode) mapper.readTree("{\"Teacher\":{\"osid\":\"09cc3c81-6180-4e74-aba9-f015bbaa95f1\",\"basicProficiencyLevel\":[{\"osid\":\"1a496e91-7886-44e2-abfa-1a40c3337d1e\"}],\"childObj\":{\"osid\":\"4a497b91-7886-44e2-abfa-1a40c3337d1f\"}}}");
        } catch (Exception e) {
        }

        JSONUtil.trimPrefix(testDataNode, "1-");
        assertEquals(expected, testDataNode);
    }

    @Test
    void addPrefix() {
        ObjectNode actualNode = getNodeData(ACTUAL_DATA);
        ObjectNode expectedNode = getNodeData(EXPECTED_ADDED_PREFIX_DATA);
        List<String> keys = new ArrayList<>();
        keys.add(Constants.JsonldConstants.ID);

        JSONUtil.addPrefix(actualNode, "prefix-", keys);
        assertEquals(actualNode, expectedNode);
    }

    @Test
    void addNode_noKey_throws_exception() {
        ObjectNode actualNode = getNodeData(ACTUAL_DATA);
        assertThrows(IllegalArgumentException.class, () -> {
            JSONUtil.addNode(actualNode, "", JsonNodeFactory.instance.objectNode());
        });
    }

    @Test
    void addNode() {
        ObjectNode actualNode = getNodeData(ACTUAL_DATA);
        ObjectNode expectedNode = getNodeData(EXPECTED_ADDED_NODE_DATA);

        ObjectNode nodeToAdd = JsonNodeFactory.instance.objectNode();
        nodeToAdd.put("newNodeKey", "newNodeValue");

        JSONUtil.addNode(actualNode, "addedKey", nodeToAdd);
        assertEquals(actualNode, expectedNode);
    }

    @Test
    void removeNode() {
        ObjectNode actualNode = getNodeData(ACTUAL_DATA);
        ObjectNode expectedNode = getNodeData(EXPECTED_DATA);

        JSONUtil.removeNode(actualNode, Constants.JsonldConstants.ID);
        assertEquals(actualNode, expectedNode);
    }

    @Test
    void removeNodesByPathTest() throws Exception {
        JsonNode node = mapper.readTree(new File("src/test/resources/jsonUtils-removeNodes/student.json"));
        Set<String> removalPaths = new HashSet<>(Arrays.asList(
                "$.age",
                "$.address.street",
                "$.address.pincode",
                "$.education[0]",
                "$.education[0].fromDate",
                "$.education[2]",
                "$.education[1].fromDate"
        ));
        JsonNode removeNodesByPath = JSONUtil.removeNodesByPath(node, removalPaths);
        JsonNode expected = mapper.readTree(new File("src/test/resources/jsonUtils-removeNodes/student_afterRemoval.json"));
        assertEquals(expected, removeNodesByPath);
    }

    @Test
    void diffJsonNode_test() throws IOException {
        String beforeJsonStr = "{\"a\":{\"b1\":\"c\"}}";
        String afterJsonStr = "{\"a\":{\"b1\":\"d\",\"b2\":\"d\"}}";
        JsonNode beforeNode = mapper.readTree(beforeJsonStr);
        JsonNode afterNode = mapper.readTree(afterJsonStr);
        JsonNode patchNode = JSONUtil.diffJsonNode(beforeNode, afterNode);
        assertThat(patchNode.get(0).get("op").asText(), is("replace"));
        assertThat(patchNode.get(0).get("value").asText(), is("d"));
        assertThat(patchNode.get(1).get("op").asText(), is("add"));
        assertThat(patchNode.get(1).get("value").asText(), is("d"));
    }


    //    @Disabled
    @Test
    void diffJsonNode_WithExistingValueAsEmpty() throws IOException {
        String beforeJsonStr = "";
        String afterJsonStr = "{\"a\":{\"b1\":\"d\",\"b2\":\"d\"}}";
        JsonNode beforeNode = mapper.readTree(beforeJsonStr);
        JsonNode afterNode = mapper.readTree(afterJsonStr);
        JsonNode patchNode = JSONUtil.diffJsonNode(beforeNode, afterNode);
        assertThat(patchNode.get(0).get("op").asText(), is("add"));
    }

    @Test
    void diffJsonNode_WithBothValuesAreSame() throws IOException {
        String beforeJsonStr = "{\"a\":{\"b1\":\"d\",\"b2\":\"d\"}}";
        String afterJsonStr = "{\"a\":{\"b1\":\"d\",\"b2\":\"d\"}}";
        JsonNode beforeNode = mapper.readTree(beforeJsonStr);
        JsonNode afterNode = mapper.readTree(afterJsonStr);
        JsonNode patchNode = JSONUtil.diffJsonNode(beforeNode, afterNode);
        assertThat(patchNode.size(), is(0));
    }

    @Test
    void diffJsonNode_WithBothValuesempty() throws IOException {
        String beforeJsonStr = "";
        String afterJsonStr = "";
        JsonNode beforeNode = mapper.readTree(beforeJsonStr);
        JsonNode afterNode = mapper.readTree(afterJsonStr);
        JsonNode patchNode = JSONUtil.diffJsonNode(beforeNode, afterNode);
        assertThat(patchNode.size(), is(0));
    }

    @Test
    void convertObjectJsonNode_doNotSerializeNullValues() throws IOException {
        AuditRecord ar = new AuditRecord();
        ar.setUserId(null);
        ar.setAuditId("xyz");
        ar.setAction("action");
        ar.setTransactionId(new ArrayList<>(Arrays.asList(1234, 4566)));

        JsonNode result = JSONUtil.convertObjectJsonNode(ar);
        assertEquals(3, result.size());
        assertNull(result.get("userId"));
    }

    @Test
    void shouldReplaceFieldByJsonPointer() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode test = objectMapper.readTree(new File(TEST_DIR + "actual_data.json"));
        String jsonPtrExpr = "/Teacher/inServiceTeacherTrainingFromCRC/daysOfInServiceTeacherTraining";
        String updatedText = "test_value";
        ((ObjectNode) test.at(jsonPtrExpr.substring(0, jsonPtrExpr.lastIndexOf("/")))).put(jsonPtrExpr.substring(jsonPtrExpr.lastIndexOf("/") + 1), updatedText);
        assertEquals(updatedText, test.at(jsonPtrExpr).textValue());
    }

    @Test
    void shouldAbleToFetchPropertiesFromEntity() throws IOException {
        final String entityFilePath = "src/test/resources/property-extraction/entity.json";
        final String expectedPropertyDataFilePath = "src/test/resources/property-extraction/extracted-out.json";

        JsonNode entityNode = mapper.readTree(new File(entityFilePath));
        JsonNode expectedPropertyNode = mapper.readTree(new File(expectedPropertyDataFilePath));
        Map<String, String> attestationProperties = new HashMap<String, String>() {
            {
                put("name", "$.contactDetails.identityDetails.fullName");
                put("educationDetails", "$.educationDetails");
                put("experienceDetails", "$.experience");
            }
        };
        Map<String, List<String>> properyOSIDMapper = new HashMap<String, List<String>>() {
            {
                put("educationDetails", new ArrayList<>(Arrays.asList("1", "2")));
            }
        };
        JsonNode actualPropertyData = JSONUtil.extractPropertyDataFromEntity("osid", entityNode, attestationProperties, properyOSIDMapper);
        assertEquals(expectedPropertyNode, actualPropertyData);
    }

    private String getContent(String fileName) throws IOException {
        InputStream in;
        try {
            in = this.getClass().getClassLoader().getResourceAsStream(fileName);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8);

        } catch (IOException e) {
            logger.error("Exception occurred: {}", ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }
}