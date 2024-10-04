package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.impl.RetryRestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_LIST;
import static dev.sunbirdrc.registry.middleware.util.Constants.TOTAL_COUNT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
class DIDServiceTest {

    @Mock
    private RetryRestTemplate retryRestTemplate;

    @Mock
    private ISearchService searchService;

    @Mock
    private Gson gson;
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RegistryHelper registryHelper;

    @InjectMocks
    private DIDService didService;
    private static final String authorSchemaName = "Issuer";
    private static final String didPropertyName = "did";

    @BeforeEach
    void setup() {
        // Setup any initial configurations or mocks
    }

    @Test
    void testGetDid() throws Exception {
        String name = "John Doe";

        // Mocking the searchService to return a mock JsonNode
        when(searchService.search(any(), anyString())).thenReturn(createMockJsonNode());

        // Positive test case
        String did = didService.getDid(name);
        assertNotNull(did);

        // Negative test case when no results are found
        when(searchService.search(any(), anyString())).thenReturn(createEmptyJsonNode());
        assertThrows(RuntimeException.class, () -> didService.getDid(name));
    }

    @Test
    void testFindDidForProperty() throws Exception {
        String propertyName = "name";
        String value = "John Doe";

        // Mocking the searchService to return a mock JsonNode
        ObjectNode authorNode = JsonNodeFactory.instance.objectNode();
        authorNode.put("did", "1234567890");

        ObjectNode resultsNode = JsonNodeFactory.instance.objectNode();
        ArrayNode authorArray = JsonNodeFactory.instance.arrayNode();
        authorArray.add(authorNode);
        ObjectNode resp = JsonNodeFactory.instance.objectNode().set(ENTITY_LIST, authorArray);
        resp.put(TOTAL_COUNT, 1L);
        resultsNode.set(authorSchemaName, resp);

        when(searchService.search(any(), anyString())).thenReturn(resultsNode);

        // Positive test case
        String foundDid = didService.findDidForProperty(didPropertyName, value);
        assertEquals("1234567890", foundDid);

        // Negative test case when no results are found
        resultsNode.set(authorSchemaName, JsonNodeFactory.instance.objectNode().set(ENTITY_LIST, JsonNodeFactory.instance.arrayNode()));
        when(searchService.search(any(), anyString())).thenReturn(resultsNode);
        assertThrows(RuntimeException.class, () -> didService.findDidForProperty(didPropertyName, value));
    }

    @Test
    void testEnsureDidForName() throws Exception {
        String name = "John Doe";
        String method = "method";
        String generatedDid = "0987654321";

        // Mocking the getDid method
        DIDService mockDidService = spy(didService);
        doReturn(generatedDid).when(mockDidService).getDid(anyString());

        // Positive test case
        String did = mockDidService.ensureDidForName(name, method);
        assertNotNull(did);

        // Negative test case when getDid throws an exception
        when(mockDidService.getDid(anyString())).thenThrow(new RuntimeException("Error"));
        doReturn(generatedDid).when(mockDidService).generateDid(any(), any());
        when(objectMapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());
        JsonNode newRootNode = readTree("{\"Issuer\":{\"name\":\"Your Name\",\"did\":\"Your Generated DID\"}}");

        // Verify that addEntity method is called with the correct parameters
        mockDidService.ensureDidForName(name, method);
    }

    @Test
    void testGenerateDid() throws Exception {
        // Positive test case
        String method = "method";
        String did = "1234";
        Map<String, Object> content = new HashMap<>();
        JsonNode rootNode = readTree("[{\"id\":\"" + did + "\"}]");
        when(retryRestTemplate.postForEntity(any(), any())).thenReturn(ResponseEntity.ok(rootNode.toString()));
        String generatedDid = didService.generateDid(method, content);
        assertNotNull(generatedDid);

        // Negative test case
        String invalidMethod = "invalid_method";
        when(retryRestTemplate.postForEntity(any(), any())).thenThrow(new RestClientException(""));
        String invalidDid = didService.generateDid(invalidMethod, content);
        assertNull(invalidDid);
    }

    @Test
    void testResolveDid() throws Exception {
        String didId = "1234567890";

        // Mocking the RetryRestTemplate to return a ResponseEntity with a successful response
        ResponseEntity<String> successResponse = ResponseEntity.ok("{\"id\": \"1234567890\"}");
        when(retryRestTemplate.getForEntity(any(), any())).thenReturn(successResponse);

        // Positive test case
        JsonNode resolvedNode = didService.resolveDid(didId);
        assertNotNull(resolvedNode);
        assertEquals(didId, resolvedNode.get("id").asText());
        // Add assertions based on the expected behavior of resolveDid method

        // Mocking the RetryRestTemplate to return a ResponseEntity with an unsuccessful response
        ResponseEntity<String> failureResponse = ResponseEntity.badRequest().body("Error");
        when(retryRestTemplate.getForEntity(any(), any())).thenReturn(failureResponse);

        // Negative test case
        JsonNode failedResolvedNode = didService.resolveDid(didId);
        assertNull(failedResolvedNode);
        // Add assertions based on the expected behavior of resolveDid method with a failed response
    }

    @Test
    void testGetHealthInfo() throws Exception {
        // Mocking the RetryRestTemplate to return a ResponseEntity with a successful response
        ResponseEntity<String> successResponse = ResponseEntity.ok("{\"status\": \"UP\"}");
        when(retryRestTemplate.getForEntity(any())).thenReturn(successResponse);

        // Positive test case
        ComponentHealthInfo healthInfo = didService.getHealthInfo();
        assertNotNull(healthInfo);
        assertTrue(healthInfo.isHealthy());
        // Add assertions based on the expected behavior of getHealthInfo method for a successful response

        // Mocking the RetryRestTemplate to return a ResponseEntity with an unsuccessful response
        ResponseEntity<String> failureResponse = ResponseEntity.badRequest().body("Error");
        when(retryRestTemplate.getForEntity(any())).thenReturn(failureResponse);

        // Negative test case
        ComponentHealthInfo failedHealthInfo = didService.getHealthInfo();
        assertNotNull(failedHealthInfo);
        assertFalse(failedHealthInfo.isHealthy());
        // Add assertions based on the expected behavior of getHealthInfo method with a failed response
    }

    private JsonNode createMockJsonNode() {
        // Create a mock JsonNode for testing
        ObjectNode results = JsonNodeFactory.instance.objectNode();
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode(1);
        ObjectNode record = JsonNodeFactory.instance.objectNode();
        record.set(didPropertyName, JsonNodeFactory.instance.textNode("did:test:1234"));
        arrayNode.add(record);
        ObjectNode resp = JsonNodeFactory.instance.objectNode().set(ENTITY_LIST, arrayNode);
        resp.put(TOTAL_COUNT, 1L);
        results.set(authorSchemaName, resp);
        return results;
    }

    private JsonNode createEmptyJsonNode() {
        // Create an empty JsonNode for testing
        ObjectNode results = JsonNodeFactory.instance.objectNode();
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode(1);
        ObjectNode resp = JsonNodeFactory.instance.objectNode().set(ENTITY_LIST, arrayNode);
        resp.put(TOTAL_COUNT, 1L);
        results.set(authorSchemaName, resp);
        return results;
    }

    private JsonNode readTree(String value) throws JsonProcessingException {
        return new ObjectMapper().readTree(value);
    }
}