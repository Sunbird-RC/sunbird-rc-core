package io.opensaber.registry.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.transform.JsonToJsonLDTransformer;
import static org.junit.Assert.*;
import org.junit.Test;


import java.io.InputStreamReader;

public class TransformationClientTest {

    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void test_client_transform() throws Exception {

        String expectedResult = "{\"@type\":\"Teacher\",\"@id\":\"teacher:test_root_id\",\"teachingRole\":{\"teacherType\":{\"@id\":\"teacher:TeacherTypeCode-HEAD\"},\"appointmentType\":{\"@id\":\"teacher:TeacherAppointmentTypeCode-REGULAR\"},\"classesTaught\":[{\"@id\":\"teacher:ClassTypeCode-SECONDARYANDHIGHERSECONDARY\"}],\"appointedForSubjects\":[{\"@id\":\"teacher:SubjectCode-MATH\"}],\"mainSubjectsTaught\":[{\"@id\":\"teacher:SubjectCode-PHYSICS\"},{\"@id\":\"teacher:SubjectCode-MATH\"}],\"appointmentYear\":{\"@type\":\"xsd:gYear\",\"@value\":\"2015\"},\"@type\":\"TeachingRole\",\"@id\":\"test_teaching_role_id\"},\"basicProficiencyLevel\":[{\"proficiencySubject\":{\"@id\":\"teacher:SubjectCode-MATH\"},\"proficiencyAcademicQualification\":{\"@id\":\"teacher:AcademicQualificationTypeCode-PHD\"},\"@type\":\"BasicProficiencyLevel\",\"@id\":\"test_id_1\"},{\"proficiencySubject\":{\"@id\":\"teacher:SubjectCode-ENGLISH\"},\"proficiencyAcademicQualification\":{\"@id\":\"teacher:AcademicQualificationTypeCode-HIGHERSECONDARY\"},\"@type\":\"BasicProficiencyLevel\",\"@id\":\"test_id_2\"},{\"proficiencySubject\":{\"@id\":\"teacher:SubjectCode-SOCIALSTUDIES\"},\"proficiencyAcademicQualification\":{\"@id\":\"teacher:AcademicQualificationTypeCode-SOCIALSTUDIES\"},\"@type\":\"BasicProficiencyLevel\",\"@id\":\"test_id_3\"}],\"context\":{\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"teacher\":\"http://example.com/voc/teacher/1.0.0/\"}}";

        JsonNode inputJson = mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_data.json"))));

        RequestData<String> requestData = new RequestData<>(inputJson.toString());
        TransformationConfiguration configuration = TransformationConfiguration.builder()
                .transform(JsonToJsonLDTransformer.getInstance()).build();
        TransformationClient<String> client = new TransformationClient<>(requestData, configuration);
        ResponseData<String> response = client.transform();
        System.out.println(response.getResponseData());
        assertEquals(expectedResult, response.getResponseData());
    }
}
