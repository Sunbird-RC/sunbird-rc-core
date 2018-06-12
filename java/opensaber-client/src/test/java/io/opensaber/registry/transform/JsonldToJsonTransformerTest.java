package io.opensaber.registry.transform;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CharStreams;
import io.opensaber.registry.transform.utils.JsonUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.Map;

public class JsonldToJsonTransformerTest {

    private JsonNode mappingJson;
    private JsonNode inputJson;

    private static ObjectMapper mapper = new ObjectMapper();
    private static TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};

    private JsonldToJsonTransformer transformer = JsonldToJsonTransformer.getInstance();

    @Before
    public void initialize() throws Exception {
        mappingJson = mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonldToJsonTransformer.class.getClassLoader().getResourceAsStream("context_mapping.json"))));
        inputJson = mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_jsonld_input_data.json"))));
    }

    @Test
    public void testXsdIntegerLiteral() throws ParseException {
        JsonNode jsonNode = inputJson.path("@graph").get(0).path("teacher:serialNum");
        Map.Entry<String, JsonNode> node = new AbstractMap.SimpleEntry<>("teacher:serialNum", jsonNode);
        ObjectNode result = transformer.processLiteralsOrConstantsNode(node);
        ObjectNode expectedResult = JsonUtils.createObjectNode();
        expectedResult.put("serialNum", 12);
        assertEquals(expectedResult.toString(), result.toString());
    }

    @Test
    public void testXsdDoubleLiteral() throws ParseException {
        JsonNode jsonNode = inputJson.path("@graph").get(0).path("teacher:inServiceTeacherTrainingFromBRC").path("teacher:daysOfInServiceTeacherTraining");
        Map.Entry<String, JsonNode> node = new AbstractMap.SimpleEntry<>("teacher:daysOfInServiceTeacherTraining", jsonNode);
        ObjectNode result = transformer.processLiteralsOrConstantsNode(node);
        ObjectNode expectedResult = JsonUtils.createObjectNode();
        expectedResult.put("daysOfInServiceTeacherTraining", 10.5);
        assertEquals(expectedResult.toString(), result.toString());
    }

    @Test
    public void testSchemaTextLiteral() throws ParseException {
        JsonNode jsonNode = inputJson.path("@graph").get(0).path("teacher:telephone");
        Map.Entry<String, JsonNode> node = new AbstractMap.SimpleEntry<>("teacher:telephone", jsonNode);
        ObjectNode result = transformer.processLiteralsOrConstantsNode(node);
        ObjectNode expectedResult = JsonUtils.createObjectNode();
        expectedResult.put("telephone", 7366283723L);
        assertEquals(expectedResult, result);
    }

    @Test
    public void testXsdDate() throws ParseException {
        JsonNode jsonNode = inputJson.path("@graph").get(0).path("teacher:birthDate");
        Map.Entry<String, JsonNode> node = new AbstractMap.SimpleEntry<>("teacher:birthDate", jsonNode);
        ObjectNode result = transformer.processLiteralsOrConstantsNode(node);
        ObjectNode expectedResult = JsonUtils.createObjectNode();
        expectedResult.put("birthDate", "1990-12-06");
        assertEquals(expectedResult.toString(), result.toString());
    }

    @Test
    public void testArrayNode() throws ParseException {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add("test1").add("test2").add("test3");
        JsonUtils.createObjectNode().putArray("array_field").addAll(arrayNode);
        Map.Entry<String, JsonNode> node = new AbstractMap.SimpleEntry<>("teacher:array_field", arrayNode);
        ObjectNode result = transformer.processLiteralsOrConstantsNode(node);
        assertEquals("{\"array_field\":[\"test1\",\"test2\",\"test3\"]}", result.toString());
    }

    @Test
    public void testEnumeratedConstantNode() throws ParseException {
        JsonNode jsonNode = inputJson.path("@graph").get(0).path("teacher:gender");
        Map.Entry<String, JsonNode> node = new AbstractMap.SimpleEntry<>("teacher:gender", jsonNode);
        ObjectNode result = transformer.processLiteralsOrConstantsNode(node);
        ObjectNode expectedResult = JsonUtils.createObjectNode();
        expectedResult.put("gender", "GenderTypeCode-FEMALE");
        assertEquals(expectedResult.toString(), result.toString());
    }

    @Test
    public void testArrayEnumeratedConstantNode() throws ParseException {
        JsonNode jsonNode = inputJson.path("@graph").get(0).path("teacher:teachingRole").path("teacher:mainSubjectsTaught");
        Map.Entry<String, JsonNode> node = new AbstractMap.SimpleEntry<>("teacher:mainSubjectsTaught", jsonNode);
        ObjectNode result = transformer.processLiteralsOrConstantsNode(node);
        ObjectNode expectedResult = JsonUtils.createObjectNode();
        ArrayNode values = JsonNodeFactory.instance.arrayNode();
        values.add("SubjectCode-PHYSICS").add("SubjectCode-MATH");
        expectedResult.putArray("mainSubjectsTaught").addAll(values);
        assertEquals(expectedResult.toString(), result.toString());
    }

    @Test
    public void testComplexNode() throws ParseException, IOException {
        ObjectNode result = transformer.constructJson(inputJson, mappingJson);
        JsonNode expectedResult = mapper.readTree(CharStreams.toString(new InputStreamReader(
                        JsonldToJsonTransformer.class.getClassLoader().getResourceAsStream("teacher_json_output.json"))));
        assertEquals(expectedResult.toString(), result.toString());
    }

    @Test
    public void testArrayOfComplexNode() throws ParseException, IOException {
        JsonNode jsonNode = inputJson.path("@graph").get(0).path("teacher:basicProficiencyLevel");
        Map.Entry<String, JsonNode> node = new AbstractMap.SimpleEntry<>("teacher:basicProficiencyLevel", jsonNode);
        Map<String, String> context = mapper.readValue(mappingJson.path("context").toString(), typeRef);
        ObjectNode result = transformer.processComplexNode(node, mappingJson, context);
        String expectedResult = "{\"teacher:basicProficiencyLevel\":[{\"proficiencySubject\":\"SubjectCode-MATH\",\"proficiencyAcademicQualification\":\"AcademicQualificationTypeCode-PHD\",\"id\":\"http://localhost:8080/66af06f5-32bf-4dbb-9755-98cea56539e5\"},{\"proficiencySubject\":\"SubjectCode-SOCIALSTUDIES\",\"proficiencyAcademicQualification\":\"AcademicQualificationTypeCode-SECONDARY\",\"id\":\"http://localhost:8080/ed4c6897-b496-4aeb-9e04-b390914e3e6f\"},{\"proficiencySubject\":\"SubjectCode-ENGLISH\",\"proficiencyAcademicQualification\":\"AcademicQualificationTypeCode-HIGHERSECONDARY\",\"id\":\"http://localhost:8080/b6bbb5da-2b28-4d47-8d7e-aac96766cd22\"}]}";
        assertEquals(expectedResult, result.toString());
    }
}
