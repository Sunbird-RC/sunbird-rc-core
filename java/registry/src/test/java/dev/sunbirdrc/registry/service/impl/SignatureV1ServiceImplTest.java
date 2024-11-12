package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestClientException;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {SignatureV1ServiceImpl.class})
@AutoConfigureMockMvc(addFilters = false)
@SpringBootTest(classes = {Gson.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
class SignatureV1ServiceImplTest {

    @MockBean
    private RetryRestTemplate retryRestTemplate;
    @MockBean
    private ObjectMapper objectMapper;
    @InjectMocks
    private SignatureV1ServiceImpl signatureV1ServiceImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void test_sign_api() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenAnswer(invocation -> ResponseEntity.accepted().body("success"));
        when(objectMapper.readTree(anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
        assertThat(signatureV1ServiceImpl.sign(Collections.emptyMap()), is(notNullValue()));
    }

    @Test
    void test_sign_api_restclient_exception() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenThrow(RestClientException.class);
        assertThrows(RestClientException.class, () -> signatureV1ServiceImpl.sign(Collections.emptyMap()));
    }

    @Test
    void test_verify_sign_with_value_as_string() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenAnswer(invocation -> ResponseEntity.accepted().body("success"));
        ObjectNode value = JsonNodeFactory.instance.objectNode();
        value.set("verified", JsonNodeFactory.instance.booleanNode(false));
        when(objectMapper.readTree(anyString())).thenReturn(value);
        assertThat(signatureV1ServiceImpl.verify(new Object()), is(notNullValue()));
    }

    @Test
    void test_verify_sign_with_restclient_exception() {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenThrow(RestClientException.class);
        assertThrows(RestClientException.class, () -> signatureV1ServiceImpl.verify(new Object()));
    }

    @Test
    void test_get_key_with_valid_keyId() throws Exception {
        when(retryRestTemplate.getForEntity(any(String.class)))
                .thenAnswer(invocation -> ResponseEntity.accepted().body("success"));
        assertThat(signatureV1ServiceImpl.getKey("2"), is(notNullValue()));
    }

    @Test
    void test_get_key_with_restclient_exception() {
        when(retryRestTemplate.getForEntity(any(String.class)))
                .thenThrow(RestClientException.class);
        assertThrows(RestClientException.class, () -> signatureV1ServiceImpl.getKey("100"));
    }

    @Test
    void test_encryption_isup() {
        when(retryRestTemplate.getForEntity(nullable(String.class)))
                .thenReturn(ResponseEntity.accepted().body("UP"));
        assertTrue(signatureV1ServiceImpl.getHealthInfo().isHealthy());
    }

    @Test
    void test_encryption_isup_throw_restclientexception() {
        when(retryRestTemplate.getForEntity(nullable(String.class)))
                .thenThrow(RestClientException.class);
        assertThrows(RestClientException.class, () -> signatureV1ServiceImpl.getHealthInfo().isHealthy());
    }
}