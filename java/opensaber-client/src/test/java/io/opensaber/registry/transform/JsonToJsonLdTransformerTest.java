package io.opensaber.registry.transform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CharStreams;
import io.opensaber.registry.exception.NodeMappingNotDefinedException;
import io.opensaber.registry.transform.utils.JsonUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

public class JsonToJsonLdTransformerTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private JsonNode mappingJson;
    private JsonNode inputJson;

    private static ObjectMapper mapper = new ObjectMapper();
    private static TypeReference<Map<String, Object>> typeRef
            = new TypeReference<Map<String, Object>>() {};
    private JsonToJsonLDTransformer transformer = JsonToJsonLDTransformer.getInstance();

    @Before
    public void initialize() throws Exception {
        mappingJson = mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("context_mapping.json"))));
        inputJson = mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_json_input_data.json"))));
    }

    @Test
    public void testProcessNode() throws NodeMappingNotDefinedException, IOException {
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = inputJson.path("teacher").fields();
        JsonNode nodeMappings = mappingJson.path("teacher").path("definition");
        ObjectNode result = JsonUtils.createObjectNode();
        ObjectNode jsonldOutput = (ObjectNode) mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_jsonld_output_data.json"))));
        ObjectNode expectedResult = JsonNodeFactory.instance.objectNode();
        expectedResult.set("teachingRole", jsonldOutput.path("teachingRole"));

        while(fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> node = fieldIterator.next();
            if (node.getKey().equalsIgnoreCase("teachingRole")) {
                Map<String, Object> mapping = mapper.readValue(nodeMappings.path(node.getKey()).toString(), typeRef);
                Map<String, Object> definitionMapping = (Map<String, Object>) mapping.get("definition");
                transformer.processNode(node, definitionMapping, mapping.get("type").toString(),
                        transformer.isMappingAComplexObject(definitionMapping), result);
                assertEquals(expectedResult, result);
            }
        }

    }

    @Test
    public void testProcessCollectionNode() throws NodeMappingNotDefinedException, IOException {
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = inputJson.path("teacher").fields();
        JsonNode nodeMappings = mappingJson.path("teacher").path("definition");
        ObjectNode result = JsonUtils.createObjectNode();
        ObjectNode jsonldOutput = (ObjectNode) mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_jsonld_output_data.json"))));
        ObjectNode expectedResult = JsonNodeFactory.instance.objectNode();
        expectedResult.set("basicProficiencyLevel", jsonldOutput.path("basicProficiencyLevel"));

        while(fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> node = fieldIterator.next();
            if (node.getKey().equalsIgnoreCase("basicProficiencyLevel")) {
                Map<String, Object> mapping = mapper.readValue(nodeMappings.path(node.getKey()).toString(), typeRef);
                transformer.processCollectionNode(node, mapping, mapping.get("type").toString(),
                        transformer.isMappingAComplexObject(mapping), result);
                assertEquals(expectedResult, result);
            }
        }
    }

    @Test
    public void testConversionWithPartialData() throws NodeMappingNotDefinedException, IOException {
        for (JsonNode node : inputJson) {
            if (node instanceof ObjectNode) {
                ObjectNode object = (ObjectNode) node;
                object.remove("basicProficiencyLevel");
            }
        }
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add("teacher:SubjectCode-ENGLISH").add("teacher:SubjectCode-MATH");
        ObjectNode teachingRoleNode = JsonNodeFactory.instance.objectNode();
        teachingRoleNode.putArray("mainSubjectsTaught").addAll(arrayNode);
        teachingRoleNode.put("id", "http://localhost:8080/5ab00f91-3c9e-4657-a9dd-622a5f8fd723");

        ((ObjectNode) inputJson.path("teacher")).set("teachingRole", teachingRoleNode);
        ObjectNode result = transformer.constructJsonLd(inputJson, mappingJson);

        ObjectNode expectedTeacherJsonldOutput = (ObjectNode) mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_jsonld_output_data.json"))));

        ObjectNode teachingRoleJsonldNode = (ObjectNode) expectedTeacherJsonldOutput.path("teachingRole");
        teachingRoleJsonldNode.remove("teacherType");
        teachingRoleJsonldNode.remove("appointmentType");
        teachingRoleJsonldNode.remove("classesTaught");
        teachingRoleJsonldNode.remove("appointedForSubjects");
        teachingRoleJsonldNode.remove("mainSubjectsTaught");
        teachingRoleJsonldNode.remove("appointmentYear");

        ArrayNode mainSubjectsTaughtJsonld = JsonNodeFactory.instance.arrayNode();
        mainSubjectsTaughtJsonld.add(JsonNodeFactory.instance.objectNode().put("@id", "teacher:teacher:SubjectCode-ENGLISH"));
        mainSubjectsTaughtJsonld.add(JsonNodeFactory.instance.objectNode().put("@id", "teacher:teacher:SubjectCode-MATH"));

        teachingRoleJsonldNode.putArray("mainSubjectsTaught").addAll(mainSubjectsTaughtJsonld);
        assertEquals(teachingRoleJsonldNode, result.path("teachingRole"));
    }

    @Test
    public void testConstructJsonld() throws NodeMappingNotDefinedException, IOException {
        ObjectNode result = transformer.constructJsonLd(inputJson, mappingJson);
        ObjectNode expectedTeacherJsonldOutput = (ObjectNode) mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_jsonld_output_data.json"))));
        assertEquals(expectedTeacherJsonldOutput, result);
    }

    @Test
    public void testNodeMappingNotFoundScenario() throws NodeMappingNotDefinedException, IOException {
        expectedEx.expect(NodeMappingNotDefinedException.class);
        expectedEx.expectMessage("");
        JsonNode invalidInputJson = mapper.readTree("{\"teacher\":{\"id\": \"test_root_id\",\"teachingInvalidRole\": {\"id\": \"test_teaching_role_id\",\"teacherType\": \"TeacherTypeCode-HEAD\",\"appointmentType\":\"TeacherAppointmentTypeCode-REGULAR\"}}}");
        ObjectNode result = transformer.constructJsonLd(invalidInputJson, mappingJson);
    }

}
