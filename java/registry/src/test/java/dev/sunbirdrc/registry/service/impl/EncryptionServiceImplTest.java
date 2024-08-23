package dev.sunbirdrc.registry.service.impl;

import com.google.gson.Gson;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.registry.exception.EncryptionException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class EncryptionServiceImplTest {

    @Mock
    private RetryRestTemplate retryRestTemplate;
    @Mock
    public SunbirdRCInstrumentation watch;
    @InjectMocks
    private EncryptionServiceImpl encryptionServiceImpl;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(encryptionServiceImpl, "encryptionEnabled", true);
        ReflectionTestUtils.setField(encryptionServiceImpl, "gson", new Gson());
    }

    @Test
    public void test_encrypt_api_with_object_as_input() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenAnswer(invocation -> ResponseEntity.ok("{\"encrypted\": \"data\"}"));
        assertThat(encryptionServiceImpl.encrypt(new Object()), is(notNullValue()));
    }

    @Test
    public void test_encrypted_api_with_map_as_input() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenAnswer(invocation -> ResponseEntity.ok("{\"encrypted\": \"data\"}"));
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("school", "BVM");
        propertyMap.put("name", "john");
        assertThat(encryptionServiceImpl.encrypt(propertyMap), is(notNullValue()));
    }

    @Test
    public void test_encrypt_api_object_param_throwing_resource_exception() {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenThrow(ResourceAccessException.class);
        org.junit.jupiter.api.Assertions.assertThrows(EncryptionException.class, () -> {
            encryptionServiceImpl.encrypt(new Object());
        });
    }

    @Test
    public void test_encrypt_api_map_param_throwing_resource_exception() {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenThrow(ResourceAccessException.class);
        org.junit.jupiter.api.Assertions.assertThrows(EncryptionException.class, () -> {
            encryptionServiceImpl.encrypt(new HashMap<>());
        });
    }

    @Test
    public void test_decrypt_api_with_object_as_input() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenAnswer(invocation -> ResponseEntity.ok("{\"decrypted\": \"data\"}"));
        assertThat(encryptionServiceImpl.decrypt(new Object()), is(notNullValue()));
    }

    @Test
    public void test_decrypt_api_object_param_throwing_resource_exception() {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenThrow(ResourceAccessException.class);
        org.junit.jupiter.api.Assertions.assertThrows(EncryptionException.class, () -> {
            encryptionServiceImpl.decrypt(new Object());
        });
    }

    @Test
    public void test_decrypt_api_with_input_as_map() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenAnswer(invocation -> ResponseEntity.ok("{\"decrypted\": \"data\"}"));
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("school", "BVM");
        propertyMap.put("name", "john");
        assertThat(encryptionServiceImpl.decrypt(propertyMap), is(notNullValue()));
    }

    @Test
    public void test_decrypt_api_map_param_throwing_resource_exception() {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class)))
                .thenThrow(ResourceAccessException.class);
        org.junit.jupiter.api.Assertions.assertThrows(EncryptionException.class, () -> {
            encryptionServiceImpl.decrypt(new HashMap<>());
        });
    }

    @Test
    public void test_encryption_isup() throws Exception {
        when(retryRestTemplate.getForEntity(nullable(String.class)))
                .thenReturn(ResponseEntity.accepted().body("{\"status\": \"UP\"}"));
        assertTrue(encryptionServiceImpl.isEncryptionServiceUp());
    }

    @Test
    public void test_encryption_isup_throw_restclientexception() {
        when(retryRestTemplate.getForEntity(nullable(String.class)))
                .thenThrow(RestClientException.class);
        assertFalse(encryptionServiceImpl.isEncryptionServiceUp());
    }
}