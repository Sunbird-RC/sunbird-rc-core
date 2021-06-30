package io.opensaber.registry.middleware.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ConditionResolverServiceTest {
    ConditionResolverService conditionResolverService = new ConditionResolverService();
    @Test
    public void shouldAbleToResolveRequesterPaths() throws IOException {
        String condition = "(ATTESTOR#$.experience.[*].institute#.contains(REQUESTER#$.educationDetails[?(@.osid == 'REQUESTER_PROPERTY_ID')]['institute']#) && (ATTESTOR#$.experience[?(@.institute == REQUESTER#$.educationDetails[?(@.osid == 'REQUESTER_PROPERTY_ID')]['institute']#)]['role'][*]#.contains('bo') || ATTESTOR#$.experience[?(@.institute == REQUESTER#$.educationDetails[?(@.osid == 'REQUESTER_PROPERTY_ID')]['institute']#)]['role'][*]#.contains('hod')))";
        String matcher = "REQUESTER";
        List<String[]> attributes = new ArrayList<String[]>(){{
            add(new String[]{"REQUESTER_PROPERTY_ID", "4"});
        }};
        String expectedCondition = "(ATTESTOR#$.experience.[*].institute#.contains('Mary school') && (ATTESTOR#$.experience[?(@.institute == 'Mary school')]['role'][*]#.contains('bo') || ATTESTOR#$.experience[?(@.institute == 'Mary school')]['role'][*]#.contains('hod')))";
        String resolve = conditionResolverService.resolve(getStudentJsonNode(), matcher, condition, attributes);
        assertEquals(expectedCondition, resolve);
    }

    @Test
    public void shouldAbleToResolveAttestorPaths() throws IOException {
        String condition = "(ATTESTOR#$.experience.[*].institute#.contains('Mary school') && (ATTESTOR#$.experience[?(@.institute == 'Mary school')]['role'][*]#.contains('bo') || ATTESTOR#$.experience[?(@.institute == 'Mary school')]['role'][*]#.contains('hod')))";
        String matcher = "ATTESTOR";
        List<String[]> attributes = new ArrayList<>();
        String expectedCondition = "({\"Mary school\",\"ABC institute of school\"}.contains('Mary school') && ({\"hod\",\"admin\"}.contains('bo') || {\"hod\",\"admin\"}.contains('hod')))";
        assertEquals(expectedCondition, conditionResolverService.resolve(getTeacherJsonNode(), matcher, condition, attributes));
    }

    @Test
    public void shouldReturnTrueForValidExpression() throws IOException {
        String condition = "(ATTESTOR#$.experience.[*].institute#.contains(REQUESTER#$.educationDetails[?(@.osid == 'REQUESTER_PROPERTY_ID')]['institute']#) && (ATTESTOR#$.experience[?(@.institute == REQUESTER#$.educationDetails[?(@.osid == 'REQUESTER_PROPERTY_ID')]['institute']#)]['role'][*]#.contains('bo') || ATTESTOR#$.experience[?(@.institute == REQUESTER#$.educationDetails[?(@.osid == 'REQUESTER_PROPERTY_ID')]['institute']#)]['role'][*]#.contains('hod')))";
        List<String[]> attributes = new ArrayList<String[]>(){{
            add(new String[]{"REQUESTER_PROPERTY_ID", "4"});
        }};
        String requester = "REQUESTER";
        String resolve = null;
        try {
            resolve = conditionResolverService.resolve(getStudentJsonNode(), requester, condition, attributes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String attestor = "ATTESTOR";
        resolve = conditionResolverService.resolve(getTeacherJsonNode(), attestor, resolve, attributes);
        assertTrue(conditionResolverService.evaluate(resolve));
    }

    @Test
    public void shouldReturnFalseForInvalidExpression() throws IOException {
        String condition = "(ATTESTOR#$.experience.[*].institute#.contains(REQUESTER#$.educationDetails[?(@.osid == 'REQUESTER_PROPERTY_ID')]['institute']#) && (ATTESTOR#$.experience[?(@.institute == REQUESTER#$.educationDetails[?(@.osid == 'REQUESTER_PROPERTY_ID')]['institute']#)]['role'][*]#.contains('boa') || ATTESTOR#$.experience[?(@.institute == REQUESTER#$.educationDetails[?(@.osid == 'REQUESTER_PROPERTY_ID')]['institute']#)]['role'][*]#.contains('hoda')))";
        List<String[]> attributes = new ArrayList<String[]>(){{
            add(new String[]{"REQUESTER_PROPERTY_ID", "4"});
        }};
        String requester = "REQUESTER";
        String resolve = conditionResolverService.resolve(getStudentJsonNode(), requester, condition, attributes);
        String attestor = "ATTESTOR";
        resolve = conditionResolverService.resolve(getTeacherJsonNode(), attestor, resolve, attributes);
        assertFalse(conditionResolverService.evaluate(resolve));
    }
    private JsonNode getTeacherJsonNode() throws IOException {
        String nodeStr = "{\n" +
                "   \"identityDetails\":{\n" +
                "      \"fullName\":\"Omen\",\n" +
                "      \"gender\":\"Male\",\n" +
                "      \"dob\":\"2021-06-10\",\n" +
                "      \"identityType\":\"Aadhar\",\n" +
                "      \"identityValue\":\"1010101\"\n" +
                "   },\n" +
                "   \"academicQualifications\":[\n" +
                "      {\n" +
                "         \"institute\":\"ABC Institute\",\n" +
                "         \"qualification\":\"10\",\n" +
                "         \"program\":\"SSLC\",\n" +
                "         \"graduationYear\":\"1990\",\n" +
                "         \"marks\":\"80\"\n" +
                "      }\n" +
                "   ],\n" +
                "   \"experience\":[\n" +
                "      {\n" +
                "         \"role\":[\n" +
                "            \"hod\",\n" +
                "            \"admin\"\n" +
                "         ],\n" +
                "         \"institute\":\"Mary school\",\n" +
                "         \"employmentType\":\"Permanent\",\n" +
                "         \"start\":\"2021-06-10\",\n" +
                "         \"end\":\"2021-06-10\",\n" +
                "         \"teacherType\":\"Head teacher\",\n" +
                "         \"subjects\":[\n" +
                "            \"science\"\n" +
                "         ],\n" +
                "         \"grades\":[\n" +
                "            \"class 6th\"\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"role\":[\n" +
                "            \"secretary\"\n" +
                "         ],\n" +
                "         \"institute\":\"ABC institute of school\",\n" +
                "         \"employmentType\":\"Permanent\",\n" +
                "         \"start\":\"2021-06-10\",\n" +
                "         \"end\":\"2021-06-10\",\n" +
                "         \"teacherType\":\"Head teacher\",\n" +
                "         \"subjects\":[\n" +
                "            \"science\"\n" +
                "         ],\n" +
                "         \"grades\":[\n" +
                "            \"7th class\"\n" +
                "         ]\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        return new ObjectMapper().readTree(nodeStr);
    }

    JsonNode getStudentJsonNode() throws IOException {
        String nodeStr = "{\n" +
                "   \"gender\":\"Male\",\n" +
                "   \"studentName\":\"Moorthy\",\n" +
                "   \"osid\":\"1-471bacc6-95ca-4cb6-b5db-8a308fdef263\",\n" +
                "   \"nationalIdentifier\":\"123456\",\n" +
                "   \"educationDetails\":[\n" +
                "      {\n" +
                "         \"degree\":\"HSC\",\n" +
                "         \"start\":\"04-05-2020\",\n" +
                "         \"end\":\"2014-09-12\",\n" +
                "         \"institute\":\"Mary school\",\n" +
                "         \"osid\":\"1-330cff35-65e6-4abd-9b0d-29fba5c97a93\",\n" +
                "         \"state\":\"DRAFT\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"degree\":\"SSLC\",\n" +
                "         \"start\":\"04-05-2020\",\n" +
                "         \"end\":\"2014-09-12\",\n" +
                "         \"institute\":\"Mary school\",\n" +
                "         \"osid\":\"4\",\n" +
                "         \"state\":\"DRAFT\"\n" +
                "      }\n" +
                "   ],\n" +
                "   \"class\":\"8th\",\n" +
                "   \"principal\":\"Samarth\"\n" +
                "}";
        return new ObjectMapper().readTree(nodeStr);
    }
}
