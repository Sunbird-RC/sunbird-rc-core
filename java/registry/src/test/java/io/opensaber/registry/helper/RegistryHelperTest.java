package io.opensaber.registry.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.keycloak.KeycloakAdminUtil;
import io.opensaber.keycloak.OwnerCreationException;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.pojos.OwnershipsAttributes;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.middleware.service.ConditionResolverService;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.service.DecryptionHelper;
import io.opensaber.registry.service.IReadService;
import io.opensaber.registry.service.ISearchService;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.registry.util.ClaimRequestClient;
import io.opensaber.registry.util.Definition;
import io.opensaber.registry.util.DefinitionsManager;
import io.opensaber.registry.util.ViewTemplateManager;
import io.opensaber.validators.IValidate;
import io.opensaber.validators.ValidationException;
import io.opensaber.workflow.KieConfiguration;
import io.opensaber.workflow.RuleEngineService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieContainer;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
@SpringBootTest(classes = {ObjectMapper.class, KieConfiguration.class})
public class RegistryHelperTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @InjectMocks
    private RegistryHelper registryHelper;

    @Autowired
    private ObjectMapper objectMapper;


    @Mock
    private ObjectMapper objectMapperMock;

    @Mock
    private ISearchService searchService;

    @Mock
    private ViewTemplateManager viewTemplateManager;

    @Mock
    private ShardManager shardManager;

    @Mock
    RegistryService registryService;

    @Mock
    IReadService readService;

    @Mock
    private DBConnectionInfoMgr dbConnectionInfoMgr;

    @Mock
    private DecryptionHelper decryptionHelper;

    @Mock
    private DefinitionsManager definitionsManager;

    @Mock
    private KeycloakAdminUtil keycloakAdminUtil;

    @Mock
    private IValidate validationService;

    @Mock
    private OpenSaberInstrumentation watch;

    @Mock
    private ConditionResolverService conditionResolverService;

    @Mock
    private ClaimRequestClient claimRequestClient;

    @Autowired
    private KieContainer kieContainer;


    private static final String INSTITUTE = "Institute";

    @Before
    public void initMocks() {
        ReflectionTestUtils.setField(registryHelper, "auditSuffix", "Audit");
        ReflectionTestUtils.setField(registryHelper, "auditSuffixSeparator", "_");
        MockitoAnnotations.initMocks(this);
        registryHelper.uuidPropertyName = "osid";
        RuleEngineService ruleEngineService = new RuleEngineService(kieContainer, keycloakAdminUtil);
        registryHelper.entityStateHelper = new EntityStateHelper(definitionsManager, ruleEngineService, conditionResolverService, claimRequestClient);
    }

    @Test
    public void getAuditLogTest() throws Exception {

        // Data creation
        String inputJson = "{\"Teacher\":{ \"filters\":{ \"recordId\":{\"eq\":\"12c61cc3-cc6a-4a96-8409-e506fb26ddbb\"} } } }";

        String result = "{ \"Teacher_Audit\": [{ \"auditId\": \"66fecb96-b87c-44b5-a930-3de96503aa13\", \"recordId\": \"12c61cc3-cc6a-4a96-8409-e506fb26ddbb\","
                + " \"timeStamp\": \"2019-12-23 16:56:50.905\", \"date\": 1578566074000, \"@type\": \"Teacher_Audit\", \"action\": \"ADD\", "
                + "\"auditJson\": [ \"op\", \"path\" ], \"osid\": \"1-d28fd315-bc28-4db0-b7f8-130ff164ba01\", \"userId\": \"35448199-0a7b-4491-a796-b053b9fcfd29\","
                + " \"transactionId\": [ 870924631 ] }] }";

        JsonNode jsonNode = null;
        JsonNode resultNode = null;
        jsonNode = objectMapper.readTree(inputJson);
        resultNode = objectMapper.readTree(result);

        when(objectMapperMock.createArrayNode()).thenReturn(objectMapper.createArrayNode());
        when(searchService.search(ArgumentMatchers.any())).thenReturn(resultNode);
        when(viewTemplateManager.getViewTemplate(ArgumentMatchers.any())).thenReturn(null);

        JsonNode node = registryHelper.getAuditLog(jsonNode);
        assertEquals(jsonNode.get("Teacher").get("filters").get("recordId").get("eq"), node.get("Teacher_Audit").get(0).get("recordId"));
    }

    @Test
    public void shouldAbleToGetThePropertyIdForTheRequestBody() throws Exception {
        String entityName = "Student";
        String entityId = "";
        JsonNode requestBody = new ObjectMapper().readTree("{\n" +
                "    \"program\": \"test123\",\n" +
                "    \"graduationYear\": \"2021\",\n" +
                "    \"marks\": \"78\",\n" +
                "    \"institute\": \"DC universe\"\n" +
                "}\n");
        String propertyURI = "educationDetails";
        ObjectNode student = new ObjectMapper().createObjectNode();
        JsonNode studentNodeContent = new ObjectMapper().readTree("{\n" +
                "        \"educationDetails\": [\n" +
                "            {\n" +
                "                \"graduationYear\": \"2022\",\n" +
                "                \"institute\": \"CD universe\",\n" +
                "                \"osid\": \"1-8d6dfb25-7789-44da-a6d4-eacf93e3a7bb\",\n" +
                "                \"program\": \"8th\",\n" +
                "                \"marks\": \"99\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"graduationYear\": \"2021\",\n" +
                "                \"institute\": \"DC universe\",\n" +
                "                \"osid\": \"1-7d9dfb25-7789-44da-a6d4-eacf93e3a7aa\",\n" +
                "                \"program\": \"test123\",\n" +
                "                \"marks\": \"78\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"contactDetails\": {\n" +
                "            \"osid\": \"1-096cd663-6ba9-49f8-af31-1ace9e31bc31\",\n" +
                "            \"mobile\": \"9000090000\",\n" +
                "            \"osOwner\": \"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\",\n" +
                "            \"email\": \"ram@gmail.com\"\n" +
                "        },\n" +
                "        \"osid\": \"1-b4907dc2-d3a8-49dc-a933-2b473bdd2ddb\",\n" +
                "        \"identityDetails\": {\n" +
                "            \"osid\": \"1-9f50f1b3-99cc-4fcb-9e51-e0dbe0be19f9\",\n" +
                "            \"gender\": \"Male\",\n" +
                "            \"identityType\": \"\",\n" +
                "            \"dob\": \"1999-01-01\",\n" +
                "            \"fullName\": \"First Avenger\",\n" +
                "            \"identityValue\": \"\",\n" +
                "            \"osOwner\": \"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\"\n" +
                "        },\n" +
                "        \"osOwner\": \"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\"\n" +
                "    }");
        student.set("Student", studentNodeContent);
        when(readService.getEntity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(student);
        String propertyId = registryHelper.getPropertyIdAfterSavingTheProperty(entityName, entityId, requestBody, propertyURI);
        String actualPropertyId = "1-7d9dfb25-7789-44da-a6d4-eacf93e3a7aa";
        assertEquals(propertyId, actualPropertyId);
    }

    @Captor
    ArgumentCaptor<Shard> shardCapture;
    @Captor
    ArgumentCaptor<String> userIdCapture;
    @Captor
    ArgumentCaptor<JsonNode> inputJsonCapture;

    @Test
    public void shouldCreateOwnersForInvite() throws Exception {
        JsonNode inviteJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"instituteName\":\"gecasu\"}}");
        when(definitionsManager.getOwnershipAttributes(INSTITUTE))
                .thenReturn(Collections.singletonList(
                        OwnershipsAttributes.builder().mobile("").email("/email").userId("/email").build()
                ));
        when(definitionsManager.getDefinition(INSTITUTE))
                .thenReturn(new Definition(new ObjectMapper().readTree("{\"title\":\"Institute\",\"definitions\":{\"Institute\":{\"$id\":\"#\\/properties\\/Institute\",\"title\":\"The Institute Schema\",\"required\":[],\"properties\":{\"contactNumber\":{\"$id\":\"#\\/properties\\/contactNumber\",\"type\":\"string\",\"title\":\"Landline \\/ Mobile\"},\"email\":{\"$id\":\"#\\/properties\\/email\",\"type\":\"string\",\"format\":\"email\",\"title\":\"Email\"}}}},\"_osConfig\":{\"privateFields\":[],\"signedFields\":[],\"indexFields\":[],\"uniqueIndexFields\":[],\"systemFields\":[\"osCreatedAt\",\"osUpdatedAt\",\"osCreatedBy\",\"osUpdatedBy\"],\"attestationPolicies\":[],\"subjectJsonPath\":\"email\",\"ownershipAttributes\":[{\"email\":\"\\/email\",\"mobile\":\"\\/contactNumber\",\"userId\":\"\\/email\"},{\"email\":\"\\/adminEmail\",\"mobile\":\"\\/adminMobile\",\"userId\":\"\\/adminEmail\"}]}}")));
        String testUserId = "be6d30e9-7c62-4a05-b4c8-ee28364da8e4";
        when(keycloakAdminUtil.createUser(any(), any(), any(), any())).thenReturn(testUserId);
        when(registryService.addEntity(any(), any(), any())).thenReturn(UUID.randomUUID().toString());
        when(shardManager.getShard(any())).thenReturn(new Shard());
        registryHelper.inviteEntity(inviteJson, "");
        Mockito.verify(registryService).addEntity(shardCapture.capture(), userIdCapture.capture(), inputJsonCapture.capture());
        assertEquals("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"instituteName\":\"gecasu\",\"osOwner\":[\"" + testUserId + "\"]}}", inputJsonCapture.getValue().toString());
    }
}