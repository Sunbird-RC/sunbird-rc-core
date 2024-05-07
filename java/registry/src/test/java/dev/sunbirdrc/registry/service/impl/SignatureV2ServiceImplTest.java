package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.dao.NotFoundException;
import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.CredentialSchemaService;
import dev.sunbirdrc.registry.service.DIDService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class SignatureV2ServiceImplTest {

    @Mock
    private RetryRestTemplate retryRestTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CredentialSchemaService credentialSchemaService;

    @Mock
    private DIDService didService;

    @InjectMocks
    private SignatureV2ServiceImpl signatureService;

    private SignatureV2ServiceImpl signatureServiceMock;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        signatureServiceMock = spy(signatureService);
    }

    @Test
    public void testSign_Success() throws Exception {
        // Prepare test data
        String title = "Test Title";
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put("key", "value");
        Object credentialTemplate = "{ \"context\": [], \"credentialSubject\": { \"property1\": \"value1\", \"property2\": \"value2\" } }";

        doReturn(null).when(signatureServiceMock).issueCredential(any(), any(), any());
        doReturn(new ObjectMapper().readTree("{ \"schema\": { \"id\": \"schemaid\", \"version\": \"1.0.0\" }}")).when(credentialSchemaService).getLatestSchemaByTags(any());

        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("data", data);
        map.put("credentialTemplate", credentialTemplate);

        try {
            signatureServiceMock.sign(map);
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }

    @Test
    public void testSign_Exception() throws Exception {
        // Prepare test data
        String title = "Test Title";
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put("key", "value");
        Object credentialTemplate = "{ \"context\": [], \"credentialSubject\": { \"property1\": \"value1\", \"property2\": \"value2\" } }";

        doThrow(new RuntimeException()).when(signatureServiceMock).issueCredential(any(), any(), any());
        doReturn(new ObjectMapper().readTree("{ \"schema\": { \"id\": \"schemaid\", \"version\": \"1.0.0\" }}")).when(credentialSchemaService).getLatestSchemaByTags(any());

        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("data", data);
        map.put("credentialTemplate", credentialTemplate);

        try {
            signatureServiceMock.sign(map);
            fail("Exception should be thrown");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testVerify_Success() throws SignatureException.VerificationException, SignatureException.UnreachableException, IOException {
        // Prepare test data
        ObjectNode credential = JsonNodeFactory.instance.objectNode();
        credential.put("credentialId", "12345");

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("verified", "true");

        doReturn(result).when(signatureServiceMock).verifyCredential(any());
        assertTrue(signatureServiceMock.verify(Collections.singletonMap("credentialId", "12345")));

        result.put("verified", "false");
        assertFalse(signatureServiceMock.verify(Collections.singletonMap("credentialId", "12345")));
    }

    @Test
    public void testVerify_Exception() throws Exception {
        // Prepare test data
        ObjectNode credential = JsonNodeFactory.instance.objectNode();
        credential.put("credentialId", "12345");

        doThrow(new IOException()).when(signatureServiceMock).verifyCredential(any());
        try {
            signatureServiceMock.verify(Collections.singletonMap("credentialId", "12345"));
            fail("Exception should be thrown");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testGetKey_success() throws Exception {
        String keyId = "did:1234";
        JsonNode didDocument = new ObjectMapper().readTree("{\"verificationMethod\": [{\"id\": \"did:1234\"}]}");
        when(didService.resolveDid(keyId)).thenReturn(didDocument);
        assertEquals(new ObjectMapper().readTree(signatureService.getKey(keyId)).get("id").asText(), keyId);
    }

    @Test
    public void testGetKey_not_exists() throws Exception {
        String keyId = "did:1234";
        JsonNode didDocument = new ObjectMapper().readTree("{\"verificationMethod\": [{\"id\": \"did:12345\"}]}");
        when(didService.resolveDid(keyId)).thenReturn(didDocument);
        assertNull(signatureService.getKey(keyId));
    }

    @Test
    public void restRevoke_success() throws Exception {
        String credentialId = "did:1234";
        doNothing().when(retryRestTemplate).deleteForEntity(any(), eq(credentialId));
        try {
            signatureService.revoke("", "", credentialId);
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }

    @Test
    public void testRevoke_Exception() throws Exception {
        String credentialId = "did:1234";
        doThrow(new RuntimeException("Not Valid")).when(retryRestTemplate).deleteForEntity(any(), eq(credentialId));
        try {
            signatureService.revoke("", "", credentialId);
            fail("Exception should be thrown");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testGetCertificate_application_json() throws Exception {
        // Set up test data
        String credentialId = "123";
        byte[] bytesResult = new byte[]{1,2,3,4,5};

        // Mock getCredentialById method
        String template = "abcd";
        when(retryRestTemplate.getForEntity(any(), any())).thenReturn(ResponseEntity.ok(template));
        doReturn(JsonNodeFactory.instance.objectNode()).when(signatureServiceMock).getCredentialById(any());
        doReturn(bytesResult).when(signatureServiceMock).getCredentialById(any(), any(), any(), any());

        Object result = signatureServiceMock.getCertificate(new TextNode(credentialId), null, null, "application/json", null, null, null);
        assertEquals(JsonNodeFactory.instance.objectNode(), result);

        result = signatureServiceMock.getCertificate(new TextNode(credentialId), null, null, "application/pdf", null, null, null);
        assertEquals(bytesResult, result);
    }

    @Test
    public void testGetCredentialById() throws IOException, NotFoundException {
        String credentialId = "validCredentialId";
        String responseBody = "{\"id\": \"validCredentialId\", \"name\": \"John Doe\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        ResponseEntity<String> badResponse = new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);

        // Positive test case
        when(retryRestTemplate.getForEntity(any(), any())).thenReturn(responseEntity);
        JsonNode expectedJsonNode = new ObjectMapper().readTree(responseBody);
        JsonNode actualJsonNode = signatureService.getCredentialById(credentialId);
        assertEquals(expectedJsonNode, actualJsonNode);

        // negative test case
        when(retryRestTemplate.getForEntity(any(), any())).thenReturn(badResponse);
        try {
            signatureService.getCredentialById(credentialId);
            fail("Exception should be thrown");
        } catch (NotFoundException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testGetCredentialByIdWithFormatRender() {
        // Set up test data
        String credentialId = "123";
        String format = "application/pdf";
        String templateId = "456";
        String template = "template";

        // Set up mock response
        HttpHeaders headers = new HttpHeaders();
        headers.set("templateId", templateId);
        headers.set("template", template.trim());
        headers.setAccept(Collections.singletonList(MediaType.valueOf(format)));
        byte[] responseBody = "credential".getBytes();
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(retryRestTemplate.getForObject(any(), eq(headers), eq(byte[].class), eq(credentialId))).thenReturn(responseEntity);

        // Invoke the method
        byte[] result = null;
        try {
            result = signatureService.getCredentialById(credentialId, format, templateId, template);
        } catch (IOException | NotFoundException e) {
            fail("Exception should not be thrown");
        }

        // Verify the result
        assertNotNull(result);
        assertEquals(responseBody, result);
    }

    @Test
    public void testRevocationList_success() throws IOException {
        // Set up test data
        String issuerDid = "validIssuerDid";
        Integer page = 1;
        Integer limit = 10;
        String responseBody = "[{\"id\": \"1\", \"name\": \"John Doe\"}, {\"id\": \"2\", \"name\": \"Jane Smith\"}]";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        // Mock the behavior of retryRestTemplate.getForEntity()
        when(retryRestTemplate.getForEntity(any(), any(), any(), any())).thenReturn(responseEntity);

        // Invoke the method under test
        ArrayNode result = signatureService.revocationList(issuerDid, page, limit);

        // Verify the result
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("1", result.get(0).get("id").asText());
        assertEquals("John Doe", result.get(0).get("name").asText());
        assertEquals("2", result.get(1).get("id").asText());
        assertEquals("Jane Smith", result.get(1).get("name").asText());

        // Verify the interaction with dependencies
        verify(retryRestTemplate).getForEntity(any(), eq(issuerDid), eq(page), eq(limit));
        verifyNoMoreInteractions(retryRestTemplate);
        verifyNoInteractions(objectMapper, credentialSchemaService, didService);
    }

    @Test
    public void testRevocationList_Exception() throws IOException {
        // Set up test data
        String issuerDid = "validIssuerDid";
        Integer page = 1;
        Integer limit = 10;
        String responseBody = "{\"error\": \"Invalid request\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);

        // Mock the behavior of retryRestTemplate.getForEntity()
        when(retryRestTemplate.getForEntity(any(), any(), any(), any())).thenReturn(responseEntity);

        // Invoke the method under test
        ArrayNode result = signatureService.revocationList(issuerDid, page, limit);

        // Verify the result
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify the interaction with dependencies
        verify(retryRestTemplate).getForEntity(any(), eq(issuerDid), eq(page), eq(limit));
        verifyNoMoreInteractions(retryRestTemplate);
        verifyNoInteractions(objectMapper, credentialSchemaService, didService);
    }

    @Test
    public void testGetHealthInfo() throws Exception {
        // Mocking the RetryRestTemplate to return a ResponseEntity with a successful response
        ResponseEntity<String> successResponse = ResponseEntity.ok("{\"status\": \"UP\"}");
        when(retryRestTemplate.getForEntity(any())).thenReturn(successResponse);

        // Positive test case
        ComponentHealthInfo healthInfo = signatureService.getHealthInfo();
        assertNotNull(healthInfo);
        assertTrue(healthInfo.isHealthy());
        // Add assertions based on the expected behavior of getHealthInfo method for a successful response

        // Mocking the RetryRestTemplate to return a ResponseEntity with an unsuccessful response
        ResponseEntity<String> failureResponse = ResponseEntity.badRequest().body("Error");
        when(retryRestTemplate.getForEntity(any())).thenReturn(failureResponse);

        // Negative test case
        ComponentHealthInfo failedHealthInfo = signatureService.getHealthInfo();
        assertNotNull(failedHealthInfo);
        assertFalse(failedHealthInfo.isHealthy());
        // Add assertions based on the expected behavior of getHealthInfo method with a failed response
    }

    // Add similar tests for other methods like getKey, revoke, getCertificate, etc.
}