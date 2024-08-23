package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {Gson.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class SignatureV1ServiceImplTest {

    @Mock
    private RetryRestTemplate retryRestTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private SignatureV1ServiceImpl signatureV1ServiceImpl;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(signatureV1ServiceImpl, "signatureEnabled", true);
    }

    @Test
    public void test_sign_api() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenAnswer(invocation -> ResponseEntity.accepted().body("success"));
        when(objectMapper.readTree(anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
        assertThat(signatureV1ServiceImpl.sign(Collections.emptyMap()), is(notNullValue()));
    }

    @Test
    public void test_sign_api_restclient_exception() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenThrow(RestClientException.class);
        assertThrows(RestClientException.class, () -> signatureV1ServiceImpl.sign(Collections.emptyMap()));
    }

    @Test
    public void test_verify_sign_with_value_as_string() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenAnswer(invocation -> ResponseEntity.accepted().body("success"));
        ObjectNode value = JsonNodeFactory.instance.objectNode();
        value.set("verified", JsonNodeFactory.instance.booleanNode(false));
        when(objectMapper.readTree(anyString())).thenReturn(value);
        assertThat(signatureV1ServiceImpl.verify(new Object()), is(notNullValue()));
    }

    @Test
    public void test_verify_sign_with_restclient_exception() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenThrow(RestClientException.class);
        assertThrows(RestClientException.class, () -> signatureV1ServiceImpl.verify(new Object()));
    }

    @Test
    public void test_get_key_with_valid_keyId() throws Exception {
        when(retryRestTemplate.getForEntity(any(String.class)))
                .thenAnswer(invocation -> ResponseEntity.accepted().body("success"));
        assertThat(signatureV1ServiceImpl.getKey("2"), is(notNullValue()));
    }

    @Test
    public void test_get_key_with_restclient_exception() throws Exception {
        when(retryRestTemplate.getForEntity(any(String.class)))
                .thenThrow(RestClientException.class);
        assertThrows(RestClientException.class, () -> signatureV1ServiceImpl.getKey("100"));
    }

    @Test
    public void test_encryption_isup() throws Exception {
        when(retryRestTemplate.getForEntity(nullable(String.class)))
                .thenReturn(ResponseEntity.accepted().body("UP"));
        assertTrue(signatureV1ServiceImpl.getHealthInfo().isHealthy());
    }

    @Test
    public void test_encryption_isup_throw_restclientexception() throws Exception {
        when(retryRestTemplate.getForEntity(nullable(String.class)))
                .thenThrow(RestClientException.class);
        assertThrows(RestClientException.class, () -> signatureV1ServiceImpl.getHealthInfo().isHealthy());
    }
}