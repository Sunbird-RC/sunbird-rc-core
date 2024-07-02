package dev.sunbirdrc.registry.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.pojos.UniqueIdentifierField;
import dev.sunbirdrc.registry.exception.CustomException;
import dev.sunbirdrc.registry.exception.UniqueIdentifierException.GenerateException;
import dev.sunbirdrc.registry.exception.UniqueIdentifierException.IdFormatException;
import dev.sunbirdrc.registry.exception.UniqueIdentifierException.UnreachableException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

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

    @Test(expected = GenerateException.class)
    public void testGenerateIdFailure() throws CustomException, IOException {
        List<UniqueIdentifierField> fields = new ArrayList<>();

        HttpEntity<String> entity = new HttpEntity<>("request");

        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(any(), eq(entity))).thenReturn(new ResponseEntity<>("{\"responseInfo\":{\"status\":\"FAILED\"}}", HttpStatus.OK));

        idGenService.generateId(fields);
    }

    @Test(expected = UnreachableException.class)
    public void testGenerateIdResourceAccessException() throws CustomException {
        List<UniqueIdentifierField> fields = new ArrayList<>();
        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(eq(generateUrl), any(HttpEntity.class))).thenThrow(new ResourceAccessException("Exception"));

        idGenService.generateId(fields);
    }

    @Test
    public void testSaveIdFormatSuccessful() throws CustomException, IOException {
        List<UniqueIdentifierField> fields = new ArrayList<>();

        HttpEntity<String> entity = new HttpEntity<>("request");

        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(eq(idFormatUrl), any(HttpEntity.class))).thenReturn(new ResponseEntity<>("{\"responseInfo\":{\"status\":\"SUCCESSFUL\"}}", HttpStatus.OK));
        idGenService.saveIdFormat(fields);
    }

    @Test(expected = GenerateException.class)
    public void testSaveIdFormatFailure() throws CustomException, IOException {
        List<UniqueIdentifierField> fields = new ArrayList<>();
        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(eq(idFormatUrl), any(HttpEntity.class))).thenReturn(new ResponseEntity<>("{\"responseInfo\":{\"status\":\"FAILED\"},\"errorMsgs\":[\"Some error\"]}", HttpStatus.OK));
        idGenService.saveIdFormat(fields);
    }

    @Test(expected = UnreachableException.class)
    public void testSaveIdFormatResourceAccessException() throws CustomException {
        List<UniqueIdentifierField> fields = new ArrayList<>();
        when(gson.toJson(anyMap())).thenReturn("request");
        when(retryRestTemplate.postForEntity(eq(idFormatUrl), any(HttpEntity.class))).thenThrow(new ResourceAccessException("Exception"));
        idGenService.saveIdFormat(fields);
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
