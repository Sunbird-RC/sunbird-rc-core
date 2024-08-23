package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.impl.RetryRestTemplate;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class CredentialSchemaServiceTest {

    @Mock
    private DIDService didService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RetryRestTemplate retryRestTemplate;
    @Mock
    private IDefinitionsManager definitionsManager;
    @InjectMocks
    private CredentialSchemaService credentialSchemaService;
    @Mock
    private CredentialSchemaService credentialSchemaServiceMock;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        credentialSchemaServiceMock = spy(credentialSchemaService);
    }

    @Test
    public void test_valid_title_and_credential_template() throws IOException {
        // Given
        String title = "Test100";
        String credTemplate = "{ \"context\": [], \"credentialSubject\": { \"property1\": \"value1\", \"property2\": \"value2\" } }";

        // When
        JsonNode result = credentialSchemaService.convertCredentialTemplateToSchema(title, credTemplate);

        // Then
        assertEquals("https://w3c-ccg.github.io/vc-json-schemas/", result.get("type").asText());
        assertEquals("1.0.0", result.get("version").asText());
        assertEquals("Proof of Test100 Credential", result.get("name").asText());
        assertEquals("Proof-of-Test100-Credential", result.get("schema").get("$id").asText());
        assertEquals("object", result.get("schema").get("type").asText());
        assertEquals("string", result.get("schema").get("properties").get("property1").get("type").asText());
        assertEquals("string", result.get("schema").get("properties").get("property2").get("type").asText());
        assertEquals(2, result.get("schema").get("required").size());
        assertEquals("property1", result.get("schema").get("required").get(0).asText());
        assertEquals("property2", result.get("schema").get("required").get(1).asText());
    }

    @Test
    public void test_empty_title_and_valid_credential_template() throws IOException {
        String title = "Test";
        Object credTemplate = new LinkedHashMap<>();
        ((LinkedHashMap<String, Object>) credTemplate).put("credentialSubject", new LinkedHashMap<>());

        when(credentialSchemaServiceMock.convertCredentialTemplateToSchema(title, credTemplate))
                .thenCallRealMethod();

        JsonNode result = credentialSchemaService.convertCredentialTemplateToSchema(title, credTemplate);
        assertEquals("Proof of Test Credential", result.get("name").asText());
        assertFalse(result.get("schema").isEmpty());
        assertTrue(result.get("schema").get("properties").isObject());
    }

    @Test
    public void test_ensure_credential_schemas() throws Exception {
        Map<String, Object> credTemplates = new HashMap<>();
        JsonNode schema1 = new ObjectMapper().readTree("{\"title\": \"Title1\", \"definitions\": { \"title\": \"Title1\", \"properties\": {} }, \"_osConfig\": {}}");
        JsonNode schema2 = new ObjectMapper().readTree("{\"title\": \"Title2\", \"definitions\": { \"title\": \"Title2\", \"properties\": {} }, \"_osConfig\": {}}");
        JsonNode schema3 = new ObjectMapper().readTree("{\"title\": \"Title3\", \"definitions\": { \"title\": \"Title3\", \"properties\": {} }, \"_osConfig\": {}}");
        Definition definition1 = new Definition(schema1);
        definition1.getOsSchemaConfiguration().setCredentialTemplate("CredTemplate1");
        credTemplates.put("Title1", "CredTemplate1");

        Definition definition2 = new Definition(schema2);
        definition2.getOsSchemaConfiguration().setCredentialTemplate("CredTemplate2");
        credTemplates.put("Title2", "CredTemplate2");

        List<AttestationPolicy> attestationPolicies = new ArrayList<>();
        AttestationPolicy attestationPolicy1 = new AttestationPolicy();
        attestationPolicy1.setName("Policy1");
        attestationPolicy1.setCredentialTemplate("CredTemplate3");
        attestationPolicies.add(attestationPolicy1);
        credTemplates.put("Title3_Policy1", "CredTemplate3");

        Definition definition3 = new Definition(schema3);
        definition3.getOsSchemaConfiguration().setAttestationPolicies(attestationPolicies);

        when(definitionsManager.getAllDefinitions()).thenReturn(Arrays.asList(definition1, definition2, definition3));
        doNothing().when(credentialSchemaServiceMock).ensureCredentialSchema(any(), any(), any());
        Field field = credentialSchemaServiceMock.getClass().getDeclaredField("definitionsManager");
        field.setAccessible(true);
        field.set(credentialSchemaServiceMock, definitionsManager);

        doCallRealMethod().when(credentialSchemaServiceMock).ensureCredentialSchemas();

        // Call the method under test
        credentialSchemaServiceMock.ensureCredentialSchemas();

        // Verify that the credential templates are retrieved correctly
        verify(definitionsManager).getAllDefinitions();
        verify(credentialSchemaServiceMock).ensureCredentialSchema(eq("Title1"), eq("CredTemplate1"), any());
        verify(credentialSchemaServiceMock).ensureCredentialSchema(eq("Title2"), eq("CredTemplate2"), any());
        verify(credentialSchemaServiceMock).ensureCredentialSchema(eq("Title3_Policy1"), eq("CredTemplate3"), any());
    }

    @Test
    public void test_getLatestSchemaByTags_success() throws Exception {
        List<String> tags = Collections.singletonList("Test Tag");
        ArrayNode schemas = JsonNodeFactory.instance.arrayNode();
        JsonNode schema1 = new ObjectMapper().readTree(
                "{\"status\": \"DRAFT\", \"schema\": { \"version\": \"1.0.0\" }}"
        );
        JsonNode schema2 = new ObjectMapper().readTree(
                "{\"status\": \"DRAFT\", \"schema\": { \"version\": \"1.1.0\" }}"
        );
        schemas.add(schema1);
        schemas.add(schema2);

        doReturn(schemas).when(credentialSchemaServiceMock).getSchemaByTags(any());
        when(credentialSchemaServiceMock.getLatestSchemaByTags(any())).thenCallRealMethod();

        JsonNode result = credentialSchemaServiceMock.getLatestSchemaByTags(tags);

        assertEquals(schema2, result);
    }

    @Test
    public void testGetSchemaByTags() throws IOException {
        // Arrange
        List<String> tags = Arrays.asList("tag1", "tag2");
        String responseBody = "[{\"schema\": {\"version\": \"1.0\", \"id\": \"123\"}, \"status\": \"ACTIVE\"}, {\"schema\": {\"version\": \"2.0\", \"id\": \"456\"}, \"status\": \"ACTIVE\"}]";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(retryRestTemplate.getForEntity(any(), any())).thenReturn(responseEntity);
        ArrayNode result = credentialSchemaService.getSchemaByTags(tags);
        assertNotNull(result);
        assertEquals(2, result.size());

        when(retryRestTemplate.getForEntity(any(), any())).thenReturn(ResponseEntity.badRequest().body(""));
        result = credentialSchemaService.getSchemaByTags(tags);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testCreateSchema() throws IOException {
        String title = "Test Title";
        JsonNode credentialSchema = JsonNodeFactory.instance.objectNode();
        String status = "DRAFT";
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("schema", credentialSchema);
        ArrayNode tags = JsonNodeFactory.instance.arrayNode();
        tags.add(title);
        node.set("tags", tags);
        node.set("status", JsonNodeFactory.instance.textNode(status));
        ResponseEntity<String> response = new ResponseEntity<>("{\"schema\": {\"title\": \"Test Title\"}}", HttpStatus.OK);
        JsonNode expectedResult = JsonNodeFactory.instance.objectNode().set("title", JsonNodeFactory.instance.textNode("Test Title"));

        when(retryRestTemplate.postForEntity(any(), any(HttpEntity.class))).thenReturn(response);
        when(objectMapper.readTree(anyString())).thenReturn(expectedResult);
        JsonNode result = credentialSchemaService.createSchema(title, credentialSchema, status);
        assertEquals(expectedResult, result);

        when(retryRestTemplate.postForEntity(any(), any(HttpEntity.class))).thenReturn(ResponseEntity.badRequest().body(""));
        try {
            credentialSchemaService.createSchema(title, credentialSchema, status);
            fail("Exception should be thrown");
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testUpdateSchema() throws IOException {
        String did = "did:example:123";
        String version = "1.0";
        String status = "ACTIVE";
        JsonNode credentialSchema = JsonNodeFactory.instance.objectNode();
        ((ObjectNode) credentialSchema).put("type", "Credential");
        ((ObjectNode) credentialSchema).put("title", "Test Credential");
        ((ObjectNode) credentialSchema).put("version", version);

        // Create expected result
        JsonNode expectedResult = JsonNodeFactory.instance.objectNode();
        JsonNode expectedResultSchema = JsonNodeFactory.instance.objectNode();
        ((ObjectNode) expectedResult).set("schema", expectedResultSchema);
        ((ObjectNode) expectedResultSchema).put("type", "Credential");
        ((ObjectNode) expectedResultSchema).put("title", "Test Credential");
        ((ObjectNode) expectedResultSchema).put("version", version);

        // Mock method calls
        when(retryRestTemplate.putForEntity(any(), any(), eq(did), eq(version))).thenReturn(ResponseEntity.ok(expectedResult.toString()));

        // Call method under test
        JsonNode result = credentialSchemaService.updateSchema(did, version, credentialSchema, status);

        // Verify the result
        assertEquals(expectedResult.get("schema"), result);
    }

    @Test
    public void testGetHealthInfo() throws Exception {
        // Mocking the RetryRestTemplate to return a ResponseEntity with a successful response
        ResponseEntity<String> successResponse = ResponseEntity.ok("{\"status\": \"UP\"}");
        when(retryRestTemplate.getForEntity(any())).thenReturn(successResponse);

        // Positive test case
        ComponentHealthInfo healthInfo = credentialSchemaService.getHealthInfo();
        assertNotNull(healthInfo);
        assertTrue(healthInfo.isHealthy());
        // Add assertions based on the expected behavior of getHealthInfo method for a successful response

        // Mocking the RetryRestTemplate to return a ResponseEntity with an unsuccessful response
        ResponseEntity<String> failureResponse = ResponseEntity.badRequest().body("Error");
        when(retryRestTemplate.getForEntity(any())).thenReturn(failureResponse);

        // Negative test case
        ComponentHealthInfo failedHealthInfo = credentialSchemaService.getHealthInfo();
        assertNotNull(failedHealthInfo);
        assertFalse(failedHealthInfo.isHealthy());
    }
}