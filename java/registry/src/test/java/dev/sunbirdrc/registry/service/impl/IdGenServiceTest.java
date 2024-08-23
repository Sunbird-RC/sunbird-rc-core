package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.pojos.UniqueIdentifierField;
import dev.sunbirdrc.registry.exception.CustomException;
import dev.sunbirdrc.registry.exception.UniqueIdentifierException.GenerateException;
import dev.sunbirdrc.registry.exception.UniqueIdentifierException.UnreachableException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {ObjectMapper.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class IdGenServiceTest {

    @Value("${idgen.generateURL}")
    private String generateUrl;

    @Value("${idgen.idFormatURL}")
    private String idFormatUrl;

    @Value("${idgen.healthCheckURL}")
    private String healthCheckUrl;

    @Value("${idgen.tenantId}")
    private String tenantId;

    @Value("${idgen.enabled}")
    private boolean enabled;

    @Mock
    private Gson gson;

    @Mock
    private SunbirdRCInstrumentation watch;

    @Mock
    private RetryRestTemplate retryRestTemplate;

    @InjectMocks
    private IdGenService idGenService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Manually set the injected values in the mock object
        setField(idGenService, "generateUrl", generateUrl);
        setField(idGenService, "idFormatUrl", idFormatUrl);
        setField(idGenService, "healthCheckUrl", healthCheckUrl);
        setField(idGenService, "tenantId", tenantId);
        setField(idGenService, "enabled", enabled);
    }

    private void setField(Object targetObject, String fieldName, Object value) throws Exception {
        Field field = targetObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(targetObject, value);
    }

    @Test
    public void testGenerateIdSuccessful() throws CustomException, IOException {
        List<UniqueIdentifierField> fields = new ArrayList<>();
        UniqueIdentifierField field1 = new UniqueIdentifierField();
        field1.setField("field1");
        fields.add(field1);

        HttpEntity<String> entity = new HttpEntity<>("request");

        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(eq(generateUrl), any(HttpEntity.class))).thenReturn(new ResponseEntity<>("{\"responseInfo\":{\"status\":\"SUCCESSFUL\"},\"idResponses\":[{\"id\":\"1234\"}]}", HttpStatus.OK));

        Map<String, String> result = idGenService.generateId(fields);

        assertNotNull(result);
        assertEquals("1234", result.get("field1"));
    }

    @Test
    public void testGenerateIdFailure() throws CustomException, IOException {
        List<UniqueIdentifierField> fields = new ArrayList<>();

        HttpEntity<String> entity = new HttpEntity<>("request");

        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(any(), eq(entity))).thenReturn(new ResponseEntity<>("{\"responseInfo\":{\"status\":\"FAILED\"}}", HttpStatus.OK));

        assertThrows(GenerateException.class, () -> idGenService.generateId(fields));
    }

    @Test
    public void testGenerateIdResourceAccessException() throws CustomException {
        List<UniqueIdentifierField> fields = new ArrayList<>();
        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(eq(generateUrl), any(HttpEntity.class))).thenThrow(new ResourceAccessException("Exception"));

        assertThrows(UnreachableException.class, () -> idGenService.generateId(fields));
    }

    @Test
    public void testSaveIdFormatSuccessful() throws CustomException, IOException {
        List<UniqueIdentifierField> fields = new ArrayList<>();

        HttpEntity<String> entity = new HttpEntity<>("request");

        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(eq(idFormatUrl), any(HttpEntity.class))).thenReturn(new ResponseEntity<>("{\"responseInfo\":{\"status\":\"SUCCESSFUL\"}}", HttpStatus.OK));
        idGenService.saveIdFormat(fields);
    }

    @Test
    public void testSaveIdFormatFailure() throws CustomException, IOException {
        List<UniqueIdentifierField> fields = new ArrayList<>();
        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(eq(idFormatUrl), any(HttpEntity.class))).thenReturn(new ResponseEntity<>("{\"responseInfo\":{\"status\":\"FAILED\"},\"errorMsgs\":[\"Some error\"]}", HttpStatus.OK));

        assertThrows(GenerateException.class, () -> idGenService.saveIdFormat(fields));
    }

    @Test
    public void testSaveIdFormatResourceAccessException() throws CustomException {
        List<UniqueIdentifierField> fields = new ArrayList<>();
        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(eq(idFormatUrl), any(HttpEntity.class))).thenThrow(new ResourceAccessException("Exception"));

        assertThrows(UnreachableException.class, () -> idGenService.saveIdFormat(fields));
    }

    @Test
    public void testGetHealthInfoWhenHealthy() throws IOException {
        when(retryRestTemplate.getForEntity(any())).thenReturn(new ResponseEntity<>("{\"status\":\"UP\"}", HttpStatus.OK));
        ComponentHealthInfo healthInfo = idGenService.getHealthInfo();
        assertNotNull(healthInfo);
        assertTrue(healthInfo.isHealthy());
    }

    @Test
    public void testGetHealthInfoWhenUnhealthy() throws IOException {
        when(retryRestTemplate.getForEntity(any())).thenReturn(new ResponseEntity<>("{\"status\":\"DOWN\"}", HttpStatus.OK));
        ComponentHealthInfo healthInfo = idGenService.getHealthInfo();
        assertNotNull(healthInfo);
        assertFalse(healthInfo.isHealthy());
    }
}