package io.opensaber.validators.json.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.util.Definition;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

public class JsonValidationServiceImplTest {

    private JsonValidationServiceImpl jsonValidationService;

    private static Definition schemaDefinition;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String schemaDirectory = "classpath://validation-test/_schemas/";

    private static final String sampleSchemaPath = "src/test/resources/validation-test/_schemas/Student.json";
    private static final String sampleJsonPathOnlyRequiredErrors = "src/test/resources/validation-test/records/student1.json";
    private static final String sampleJsonPathRequiredSchemaErrors = "src/test/resources/validation-test/records/student2.json";

    private JsonNode jsonObj;

    @Before
    public void setUp() throws IOException {
        schemaDefinition = new Definition(mapper.readTree(new File(sampleSchemaPath)));


        jsonValidationService = new JsonValidationServiceImpl(schemaDirectory);
        jsonValidationService.addDefinitions(schemaDefinition.getTitle(), schemaDefinition.getContent());
    }

    @Test(expected = MiddlewareHaltException.class)
    public void testValidate() throws Exception {
        jsonObj = mapper.readTree(new File(sampleJsonPathOnlyRequiredErrors));
        jsonValidationService.validate(schemaDefinition.getTitle(), mapper.writeValueAsString(jsonObj), false);
    }

    @Test
    public void testIgnoreRequiredValidation() throws Exception {
        jsonObj = mapper.readTree(new File(sampleJsonPathOnlyRequiredErrors));
        jsonValidationService.validate(schemaDefinition.getTitle(), mapper.writeValueAsString(jsonObj), true);
    }

    @Test(expected = MiddlewareHaltException.class)
    public void testIgnoreRequiredValidationWithSchemaViolations() throws Exception {
        jsonObj = mapper.readTree(new File(sampleJsonPathRequiredSchemaErrors));
        jsonValidationService.validate(schemaDefinition.getTitle(), mapper.writeValueAsString(jsonObj), true);
    }
}