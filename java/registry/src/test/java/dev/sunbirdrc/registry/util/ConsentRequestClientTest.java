package dev.sunbirdrc.registry.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.middleware.util.OSSystemFields;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_TYPE;
import static dev.sunbirdrc.registry.middleware.util.Constants.FILTERS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ConsentRequestClientTest {

    @Mock
    private RegistryHelper registryHelper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    RestTemplate restTemplate;
    @InjectMocks
    private ConsentRequestClient consentRequestClient;
    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(consentRequestClient, "consentUrl", "localhost:8083");
        ReflectionTestUtils.setField(consentRequestClient, "registryHelper", registryHelper);
        ReflectionTestUtils.setField(consentRequestClient, "objectMapper", objectMapper);

    }

    @Test
    public void shouldCallGetConsentByIdAndCreator() {
        consentRequestClient.getConsentByConsentIdAndCreator("123", "456");
        verify(restTemplate, times(1)).getForObject(
                "localhost:8083/api/v1/consent/123/456", JsonNode.class);
    }

    @Test
    public void shouldSearchUser() throws Exception {
        ObjectNode expectedPayload = JsonNodeFactory.instance.objectNode();
        expectedPayload.set(ENTITY_TYPE, JsonNodeFactory.instance.arrayNode().add("temp"));
        ObjectNode filters = JsonNodeFactory.instance.objectNode();
        filters.set(OSSystemFields.osOwner.toString(), JsonNodeFactory.instance.objectNode().put("contains", "123"));
        expectedPayload.set(FILTERS, filters);
        consentRequestClient.searchUser("temp", "123");
        verify(registryHelper, times(1)).searchEntity(expectedPayload);
    }

    @Test
    public void shouldGetConsentByOwner() {
        consentRequestClient.getConsentByOwner("123");
        verify(restTemplate, times(1)).getForObject("localhost:8083/api/v1/consent/owner/123", JsonNode.class);
    }
}
