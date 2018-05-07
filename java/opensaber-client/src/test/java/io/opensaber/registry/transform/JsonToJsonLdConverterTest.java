package io.opensaber.registry.transform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CharStreams;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

public class JsonToJsonLdConverterTest {

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
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_data.json"))));
    }

    @Test
    public void test_process_node() throws Exception {
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = inputJson.path("teacher").fields();
        JsonNode nodeMappings = mappingJson.path("teacher").path("definition");
        ObjectNode result = transformer.createObjectNode();
        String expectedResult = "{\"teachingRole\":{\"teacherType\":{\"@id\":\"teacher:TeacherTypeCode-HEAD\"},\"appointmentType\":{\"@id\":\"teacher:TeacherAppointmentTypeCode-REGULAR\"},\"classesTaught\":[{\"@id\":\"teacher:ClassTypeCode-SECONDARYANDHIGHERSECONDARY\"}],\"appointedForSubjects\":[{\"@id\":\"teacher:SubjectCode-MATH\"}],\"mainSubjectsTaught\":[{\"@id\":\"teacher:SubjectCode-PHYSICS\"},{\"@id\":\"teacher:SubjectCode-MATH\"}],\"appointmentYear\":{\"@type\":\"xsd:gYear\",\"@value\":\"2015\"},\"@type\":\"TeachingRole\",\"@id\":\"test_teaching_role_id\"}}";

        while(fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> node = fieldIterator.next();
            if (node.getKey().equalsIgnoreCase("teachingRole")) {
                Map<String, Object> mapping = mapper.readValue(nodeMappings.path(node.getKey()).toString(), typeRef);
                Map<String, Object> definitionMapping = (Map<String, Object>) mapping.get("definition");
                transformer.processNode(node, definitionMapping, mapping.get("type").toString(),
                        transformer.isMappingAComplexObject(definitionMapping), result);
                assertEquals(expectedResult.trim(), result.toString());
            }
        }

    }

    @Test
    public void test_process_collection_node() throws Exception {
        Iterator<Map.Entry<String, JsonNode>> fieldIterator = inputJson.path("teacher").fields();
        JsonNode nodeMappings = mappingJson.path("teacher").path("definition");
        ObjectNode result = transformer.createObjectNode();
        String expectedResult = "{\"basicProficiencyLevel\":[{\"proficiencySubject\":{\"@id\":\"teacher:SubjectCode-MATH\"},\"proficiencyAcademicQualification\":{\"@id\":\"teacher:AcademicQualificationTypeCode-PHD\"},\"@type\":\"BasicProficiencyLevel\",\"@id\":\"test_id_1\"},{\"proficiencySubject\":{\"@id\":\"teacher:SubjectCode-ENGLISH\"},\"proficiencyAcademicQualification\":{\"@id\":\"teacher:AcademicQualificationTypeCode-HIGHERSECONDARY\"},\"@type\":\"BasicProficiencyLevel\",\"@id\":\"test_id_2\"},{\"proficiencySubject\":{\"@id\":\"teacher:SubjectCode-SOCIALSTUDIES\"},\"proficiencyAcademicQualification\":{\"@id\":\"teacher:AcademicQualificationTypeCode-SOCIALSTUDIES\"},\"@type\":\"BasicProficiencyLevel\",\"@id\":\"test_id_3\"}]}";

        while(fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> node = fieldIterator.next();
            if (node.getKey().equalsIgnoreCase("basicProficiencyLevel")) {
                Map<String, Object> mapping = mapper.readValue(nodeMappings.path(node.getKey()).toString(), typeRef);
                transformer.processCollectionNode(node, mapping, mapping.get("type").toString(),
                        transformer.isMappingAComplexObject(mapping), result);
                assertEquals(expectedResult.trim(), result.toString());
            }
        }
    }

    @Test
    public void test_construct_json_ld() throws Exception {
        for (JsonNode node : inputJson) {
            if (node instanceof ObjectNode) {
                ObjectNode object = (ObjectNode) node;
                object.remove("basicProficiencyLevel");
            }
        }
        String expectedResult = "{\"@type\":\"Teacher\",\"@id\":\"teacher:test_root_id\",\"teachingRole\":{\"teacherType\":{\"@id\":\"teacher:TeacherTypeCode-HEAD\"},\"appointmentType\":{\"@id\":\"teacher:TeacherAppointmentTypeCode-REGULAR\"},\"classesTaught\":[{\"@id\":\"teacher:ClassTypeCode-SECONDARYANDHIGHERSECONDARY\"}],\"appointedForSubjects\":[{\"@id\":\"teacher:SubjectCode-MATH\"}],\"mainSubjectsTaught\":[{\"@id\":\"teacher:SubjectCode-PHYSICS\"},{\"@id\":\"teacher:SubjectCode-MATH\"}],\"appointmentYear\":{\"@type\":\"xsd:gYear\",\"@value\":\"2015\"},\"@type\":\"TeachingRole\",\"@id\":\"test_teaching_role_id\"},\"context\":{\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"teacher\":\"http://example.com/voc/teacher/1.0.0/\"}}";
        ObjectNode result = transformer.constructJsonLd(inputJson, mappingJson);
        System.out.println(result);
        assertEquals(expectedResult.trim(), result.toString());
    }

}
