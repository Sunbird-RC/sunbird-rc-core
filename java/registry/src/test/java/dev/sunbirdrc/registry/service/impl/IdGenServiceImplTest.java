package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.UniqueIdentifierFields;
import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.exception.UniqueIdentifierException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.IdGenService;
import dev.sunbirdrc.registry.service.impl.RetryRestTemplate;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.OSSchemaConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.neo4j.causalclustering.core.state.machines.id.IdGenerationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.*;

public class IdGenServiceImplTest {

    @InjectMocks
    private IdGenService idGenService = new IdGenServiceImpl();

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    @Mock
    private RetryRestTemplate retryRestTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private IDefinitionsManager definitionsManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateUniqueID() throws Exception {
        // Mock the behavior of RetryRestTemplate and ObjectMapper


        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenAnswer(new Answer<ResponseEntity<String>>() {
            @Override
            public ResponseEntity<String> answer(InvocationOnMock invocation) throws Throwable {
                String response = "success";
                return ResponseEntity.accepted().body(response);
            }
        });
        when(objectMapper.readTree(anyString())).thenReturn(JsonNodeFactory.instance.objectNode());

        ObjectNode reqNode = mock(ObjectNode.class);

        // Call the method under test
        Object result = idGenService.createUniqueID(reqNode);

        // Assert the result and verify interactions
        verify(retryRestTemplate).postForEntity(anyString(), any(ObjectNode.class));
        verify(objectMapper).readTree(anyString());
        // Add more assertions based on your specific use case
    }

    @Test
    public void test_create_UniqueID_with_restclient_exception() throws Exception {
        expectedEx.expect(UniqueIdentifierException.UnreachableException.class);
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenThrow(RestClientException.class);
        ObjectNode reqNode = mock(ObjectNode.class);
        idGenService.createUniqueID(reqNode);
    }

}