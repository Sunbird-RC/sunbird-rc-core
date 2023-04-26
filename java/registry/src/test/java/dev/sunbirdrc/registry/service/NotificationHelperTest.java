package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.keycloak.KeycloakAdminUtil;
import dev.sunbirdrc.registry.helper.EntityStateHelper;
import dev.sunbirdrc.registry.middleware.service.ConditionResolverService;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.NotificationTemplate;
import dev.sunbirdrc.registry.model.NotificationTemplates;
import dev.sunbirdrc.registry.util.ClaimRequestClient;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import dev.sunbirdrc.registry.util.OSSchemaConfiguration;
import dev.sunbirdrc.workflow.KieConfiguration;
import dev.sunbirdrc.workflow.RuleEngineService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieContainer;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static dev.sunbirdrc.registry.middleware.util.Constants.EMAIL;
import static dev.sunbirdrc.registry.middleware.util.Constants.MOBILE;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
@SpringBootTest(classes = {ObjectMapper.class, KieConfiguration.class})
public class NotificationHelperTest {
    @Mock
    private RegistryService registryService;
    @Mock
    private DefinitionsManager definitionsManager;

    private ObjectMapper objectMapper;
    @Value("${notification.service.enabled}")
    boolean notificationEnabled;
    private NotificationHelper notificationHelper;
    @Mock
    EntityStateHelper entityStateHelper;
    JsonNode inputJson;
    OSSchemaConfiguration osSchemaConfiguration;
    NotificationTemplates notificationTemplates;

    @Before
    public void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        notificationHelper = new NotificationHelper(notificationEnabled, definitionsManager, entityStateHelper, registryService, objectMapper);
        osSchemaConfiguration = Mockito.mock(OSSchemaConfiguration.class);
        Definition definition = mock(Definition.class);
        when(definitionsManager.getDefinition("Institute")).thenReturn(definition);
        when(definition.getOsSchemaConfiguration()).thenReturn(osSchemaConfiguration);
        ObjectNode owners = mock(ObjectNode.class);
        inputJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"contactNumber\": \"1234123423\", \"instituteName\": \"Insitute2\", \"osid\": \"123\"}}");
        when(owners.get(MOBILE)).thenReturn(JsonNodeFactory.instance.textNode("1234123423"));
        when(owners.get(EMAIL)).thenReturn(JsonNodeFactory.instance.textNode("gecasu.ihises@tovinit.com"));
        when(entityStateHelper.getOwnersData(inputJson, "Institute")).thenReturn(Collections.singletonList(owners));
        notificationTemplates = mock(NotificationTemplates.class);
        when(osSchemaConfiguration.getNotificationTemplates()).thenReturn(notificationTemplates);
    }

    @Test
    public void shouldSendNotificationForCreateEntity() throws Exception {
        when(notificationTemplates.getCreateNotificationTemplates()).thenReturn(new NotificationTemplate("Credential Created", "{{name}}, Your {{entityType}} credential has been created"));
        doNothing().when(registryService).callNotificationActors("CREATE", "mailto:gecasu.ihises@tovinit.com", "Credential Created", ", Your Institute credential has been created");
        doNothing().when(registryService).callNotificationActors("CREATE", "tel:1234123423", "Credential Created", ", Your Institute credential has been created");
        notificationHelper.sendNotification(inputJson, "CREATE");
        verify(registryService, times(1)).callNotificationActors("CREATE", "tel:1234123423", "Credential Created", ", Your Institute credential has been created");
        verify(registryService, times(1)).callNotificationActors("CREATE", "mailto:gecasu.ihises@tovinit.com", "Credential Created", ", Your Institute credential has been created");
    }

    @Test
    public void shouldSendNotificationForUpdateEntity() throws Exception {
        JsonNode inputJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"contactNumber\": \"1234123423\", \"instituteName\": \"Insitute2\", \"osid\": \"123\"}}");
        when(notificationTemplates.getUpdateNotificationTemplates()).thenReturn(new NotificationTemplate("Credential Updated", "{{name}}, Your {{entityType}} credential has been updated"));
        doNothing().when(registryService).callNotificationActors("UPDATE", "mailto:gecasu.ihises@tovinit.com", "Credential Updated", ", Your Institute credential has been updated");
        doNothing().when(registryService).callNotificationActors("UPDATE", "tel:1234123423", "Credential Updated", ", Your Institute credential has been updated");
        notificationHelper.sendNotification(inputJson, "UPDATE");
        verify(registryService, times(1)).callNotificationActors("UPDATE", "mailto:gecasu.ihises@tovinit.com", "Credential Updated", ", Your Institute credential has been updated");
        verify(registryService, times(1)).callNotificationActors("UPDATE", "tel:1234123423", "Credential Updated", ", Your Institute credential has been updated");
    }

    @Test
    public void shouldSendNotificationForInviteEntity() throws Exception {
        when(notificationTemplates.getInviteNotificationTemplates()).thenReturn(new NotificationTemplate("Invitation", "{{name}}, You have been invited"));
        doNothing().when(registryService).callNotificationActors("INVITE", "mailto:gecasu.ihises@tovinit.com", "Invitation", ", You have been invited");
        doNothing().when(registryService).callNotificationActors("INVITE", "tel:1234123423", "Invitation", ", You have been invited");
        notificationHelper.sendNotification(inputJson, "INVITE");
        verify(registryService, times(1)).callNotificationActors("INVITE", "mailto:gecasu.ihises@tovinit.com", "Invitation", ", You have been invited");
        verify(registryService, times(1)).callNotificationActors("INVITE", "tel:1234123423", "Invitation", ", You have been invited");
    }

    @Test
    public void shouldSendNotificationForDeleteEntity() throws Exception {
        when(notificationTemplates.getDeleteNotificationTemplates()).thenReturn(new NotificationTemplate("Revoked", "{{name}}, Your credential has been revoked"));
        doNothing().when(registryService).callNotificationActors("DELETE", "mailto:gecasu.ihises@tovinit.com", "Revoked", ", Your credential has been revoked");
        doNothing().when(registryService).callNotificationActors("DELETE", "tel:1234123423", "Revoked", ", Your credential has been revoked");
        notificationHelper.sendNotification(inputJson, "DELETE");
        verify(registryService, times(1)).callNotificationActors("DELETE", "mailto:gecasu.ihises@tovinit.com", "Revoked", ", Your credential has been revoked");
        verify(registryService, times(1)).callNotificationActors("DELETE", "tel:1234123423", "Revoked", ", Your credential has been revoked");
    }
}