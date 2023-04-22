package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.keycloak.KeycloakAdminUtil;
import dev.sunbirdrc.registry.helper.EntityStateHelper;
import dev.sunbirdrc.registry.middleware.service.ConditionResolverService;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.util.ClaimRequestClient;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import dev.sunbirdrc.workflow.KieConfiguration;
import dev.sunbirdrc.workflow.RuleEngineService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieContainer;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
@SpringBootTest(classes = {ObjectMapper.class, KieConfiguration.class})
public class NotificationHelperTest {

    @Mock
    RegistryService registryService;
    private DefinitionsManager definitionsManager;
    @InjectMocks
    @Spy
    NotificationHelper notificationHelper;
    @Mock
    private ConditionResolverService conditionResolverService;

    @Mock
    private ClaimRequestClient claimRequestClient;
    @Autowired
    private KieContainer kieContainer;
    @Mock
    private KeycloakAdminUtil keycloakAdminUtil;
    RuleEngineService ruleEngineService;

    @Before
    public void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        notificationHelper.setObjectMapper(objectMapper);
        ruleEngineService = new RuleEngineService(kieContainer, keycloakAdminUtil);
        definitionsManager = new DefinitionsManager();
        Map<String, Definition> definitionMap = new HashMap<>();
        String studentSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
        String instituteSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Institute.json"), Charset.defaultCharset());
        definitionMap.put("Student", new Definition(objectMapper.readTree(studentSchema)));
        definitionMap.put("Institute", new Definition(objectMapper.readTree(instituteSchema)));
        ReflectionTestUtils.setField(definitionsManager, "definitionMap", definitionMap);
        ReflectionTestUtils.setField(notificationHelper, "definitionsManager", definitionsManager);
        notificationHelper.setEntityStateHelper(new EntityStateHelper(definitionsManager, ruleEngineService, conditionResolverService, claimRequestClient));
    }

    @Test
    public void shouldSendNotificationForCreateEntity() throws Exception {
        ReflectionTestUtils.setField(notificationHelper, "notificationEnabled", true);
        JsonNode inputJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"contactNumber\": \"1234123423\", \"instituteName\": \"Insitute2\", \"osid\": \"123\"}}");
        doNothing().when(registryService).callNotificationActors("ADD", "mailto:gecasu.ihises@tovinit.com", "Credential Created", ", Your Institute credential has been created");
        doNothing().when(registryService).callNotificationActors("ADD", "tel:", "Credential Created", ", Your Institute credential has been created");
        notificationHelper.sendNotification(inputJson, "CREATE");
        verify(registryService, times(1)).callNotificationActors("CREATE", "mailto:gecasu.ihises@tovinit.com", "Credential Created", ", Your Institute credential has been created");
        verify(registryService, times(1)).callNotificationActors("CREATE", "tel:1234123423", "Credential Created", ", Your Institute credential has been created");
    }

    @Test
    public void shouldSendNotificationForUpdateEntity() throws Exception {
        ReflectionTestUtils.setField(notificationHelper, "notificationEnabled", true);
        JsonNode inputJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"contactNumber\": \"1234123423\", \"instituteName\": \"Insitute2\", \"osid\": \"123\"}}");
        doNothing().when(registryService).callNotificationActors("UPDATE", "mailto:gecasu.ihises@tovinit.com", "Credential Updated", ", Your Institute credential has been updated");
        doNothing().when(registryService).callNotificationActors("UPDATE", "tel:", "Credential Updated", ", Your Institute credential has been updated");
        notificationHelper.sendNotification(inputJson, "UPDATE");
        verify(registryService, times(1)).callNotificationActors("UPDATE", "mailto:gecasu.ihises@tovinit.com", "Credential Updated", ", Your Institute credential has been updated");
        verify(registryService, times(1)).callNotificationActors("UPDATE", "tel:1234123423", "Credential Updated", ", Your Institute credential has been updated");
    }

    @Test
    public void shouldSendNotificationForInviteEntity() throws Exception {
        ReflectionTestUtils.setField(notificationHelper, "notificationEnabled", true);
        JsonNode inputJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"contactNumber\": \"1234123423\", \"instituteName\": \"Insitute2\", \"osid\": \"123\"}}");
        doNothing().when(registryService).callNotificationActors("INVITE", "mailto:gecasu.ihises@tovinit.com", "Invitation", ", You have been invited");
        doNothing().when(registryService).callNotificationActors("INVITE", "tel:", "Invitation", ", You have been invited");
        notificationHelper.sendNotification(inputJson, "INVITE");
        verify(registryService, times(1)).callNotificationActors("INVITE", "mailto:gecasu.ihises@tovinit.com", "Invitation", ", You have been invited");
        verify(registryService, times(1)).callNotificationActors("INVITE", "tel:1234123423", "Invitation", ", You have been invited");
    }
}