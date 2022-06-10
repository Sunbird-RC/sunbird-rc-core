package dev.sunbirdrc.registry.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.keycloak.KeycloakAdminUtil;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.middleware.service.ConditionResolverService;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.service.*;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.sink.shard.ShardManager;
import dev.sunbirdrc.registry.util.ClaimRequestClient;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import dev.sunbirdrc.registry.util.ViewTemplateManager;
import dev.sunbirdrc.validators.IValidate;
import dev.sunbirdrc.workflow.KieConfiguration;
import dev.sunbirdrc.workflow.RuleEngineService;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
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

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
@SpringBootTest(classes = {ObjectMapper.class, KieConfiguration.class})
public class RegistryHelperTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @NotNull
    private String getBaseDir() {
        return this.getClass().getResource("../../../../").getPath();
    }

    @InjectMocks
    private RegistryHelper registryHelper;

    private ObjectMapper objectMapper;

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

    private DefinitionsManager definitionsManager;


    @Mock
    private KeycloakAdminUtil keycloakAdminUtil;

    @Mock
    private IValidate validationService;

    @Mock
    private SunbirdRCInstrumentation watch;

    @Mock
    private ConditionResolverService conditionResolverService;

    @Mock
    private ClaimRequestClient claimRequestClient;

    @Autowired
    private KieContainer kieContainer;

    @Mock
    private SignatureService signatureService;


    private static final String INSTITUTE = "Institute";

    @Before
    public void initMocks() {
        objectMapper = new ObjectMapper();
        registryHelper.setObjectMapper(objectMapper);
        ReflectionTestUtils.setField(registryHelper, "auditSuffix", "Audit");
        ReflectionTestUtils.setField(registryHelper, "auditSuffixSeparator", "_");
        MockitoAnnotations.initMocks(this);
        registryHelper.uuidPropertyName = "osid";
        RuleEngineService ruleEngineService = new RuleEngineService(kieContainer, keycloakAdminUtil);
        registryHelper.entityStateHelper = new EntityStateHelper(definitionsManager, ruleEngineService, conditionResolverService, claimRequestClient);
        registryHelper.setDefinitionsManager(definitionsManager);
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

//        when(objectMapperMock.createArrayNode()).thenReturn(objectMapper.createArrayNode());
        when(searchService.search(ArgumentMatchers.any())).thenReturn(resultNode);
        when(viewTemplateManager.getViewTemplate(ArgumentMatchers.any())).thenReturn(null);

        JsonNode node = registryHelper.getAuditLog(jsonNode);
        assertEquals(jsonNode.get("Teacher").get("filters").get("recordId").get("eq"), node.get("Teacher_Audit").get(0).get("recordId"));
    }

    @Test
    public void shouldAbleToGetThePropertyIdForTheRequestBodyWhereTheExistingPropertyHasNestedObjects() throws Exception {
        String entityName = "Student";
        String entityId = "7890";
        JsonNode requestBody = new ObjectMapper().readTree("{\n" +
                "    \"program\": \"Class C\",\n" +
                "    \"graduationYear\": \"2021\",\n" +
                "    \"marks\": \"78\",\n" +
                "    \"institute\": \"b62b3d52-cffe-428d-9dd1-61ba7b0a5882\",\n" +
                "    \"documents\": [\n" +
                "        {\n" +
                "            \"fileName\": \"e3266115-0bd0-4456-a347-96f4dc335761-blog_draft\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"fileName\": \"e56dab1b-bd92-41bb-b9e5-e991438f27b8-NDEAR.txt\"\n" +
                "        }\n" +
                "    ]\n" +
                "}");
        String propertyURI = "educationDetails";
        ObjectNode student = new ObjectMapper().createObjectNode();
        JsonNode studentNodeContent = new ObjectMapper().readTree("{\n" +
                "   \"educationDetails\":[\n" +
                "      {\n" +
                "         \"osid\": \"12345\",\n" +
                "         \"program\":\"Class C\",\n" +
                "         \"graduationYear\":\"2021\",\n" +
                "         \"marks\":\"78\",\n" +
                "         \"institute\":\"b62b3d52-cffe-428d-9dd1-61ba7b0a5882\",\n" +
                "         \"documents\":[\n" +
                "            {\n" +
                "\"osid\": \"007\",\n" +
                "               \"fileName\":\"e3266115-0bd0-4456-a347-96f4dc335761-blog_draft\"\n" +
                "            },\n" +
                "            {\n" +
                "\"osid\":\"008\",\n" +
                "               \"fileName\":\"e56dab1b-bd92-41bb-b9e5-e991438f27b8-NDEAR.txt\"\n" +
                "            }\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"osid\":\"7890\",\n" +
                "         \"program\":\"Class C\",\n" +
                "         \"graduationYear\":\"2021\",\n" +
                "         \"marks\":\"78\",\n" +
                "         \"institute\":\"b62b3d52-cffe-428d-9dd1-61ba7b0a5882\",\n" +
                "         \"documents\":[\n" +
                "            {\n" +
                "         \"osid\":\"123\",\n" +
                "               \"fileName\":\"23266111-0bd0-4456-a347-96f4dc335761-blog_draft\"\n" +
                "            },\n" +
                "            {\n" +
                "         \"osid\":\"456\",\n" +
                "               \"fileName\":\"156dab12-bd92-41bb-b9e5-e991438f27b8-NDEAR.txt\"\n" +
                "            }\n" +
                "         ]\n" +
                "      }\n" +
                "   ],\n" +
                "   \"contactDetails\":{\n" +
                "      \"osid\":\"1-096cd663-6ba9-49f8-af31-1ace9e31bc31\",\n" +
                "      \"mobile\":\"9000090000\",\n" +
                "      \"osOwner\":\"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\",\n" +
                "      \"email\":\"ram@gmail.com\"\n" +
                "   },\n" +
                "   \"osid\":\"1-b4907dc2-d3a8-49dc-a933-2b473bdd2ddb\",\n" +
                "   \"identityDetails\":{\n" +
                "      \"osid\":\"1-9f50f1b3-99cc-4fcb-9e51-e0dbe0be19f9\",\n" +
                "      \"gender\":\"Male\",\n" +
                "      \"identityType\":\"\",\n" +
                "      \"dob\":\"1999-01-01\",\n" +
                "      \"fullName\":\"First Avenger\",\n" +
                "      \"identityValue\":\"\",\n" +
                "      \"osOwner\":\"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\"\n" +
                "   },\n" +
                "   \"osOwner\":\"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\"\n" +
                "}");
        student.set("Student", studentNodeContent);
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        String uri = String.format("%s/%s/%s", entityName, entityId, propertyURI);
        when(httpServletRequest.getRequestURI()).thenReturn(uri);
        when(readService.getEntity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(student);
        mockDefinitionManager();
        String propertyId = registryHelper.getPropertyIdAfterSavingTheProperty(entityName, entityId, requestBody, httpServletRequest);
        String actualPropertyId = "12345";
        Assert.assertEquals(propertyId, actualPropertyId);
    }

    @Test
    public void shouldAbleToGetThePropertyIdForTheRequestBody() throws Exception {
        String entityName = "Student";
        String entityId = "7890";
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
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        String uri = String.format("%s/%s/%s", entityName, entityId, propertyURI);
        when(httpServletRequest.getRequestURI()).thenReturn(uri);
        mockDefinitionManager();
        when(readService.getEntity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(student);
        String propertyId = registryHelper.getPropertyIdAfterSavingTheProperty(entityName, entityId, requestBody, httpServletRequest);
        String actualPropertyId = "1-7d9dfb25-7789-44da-a6d4-eacf93e3a7aa";
        assertEquals(propertyId, actualPropertyId);
    }

    @Captor
    ArgumentCaptor<Shard> shardCapture;
    @Captor
    ArgumentCaptor<String> userIdCapture;
    @Captor
    ArgumentCaptor<JsonNode> inputJsonCapture;

    @Captor
    ArgumentCaptor<String> operationCapture;
    @Captor
    ArgumentCaptor<String> toCapture;
    @Captor
    ArgumentCaptor<String> subjectCapture;
    @Captor
    ArgumentCaptor<String> messageCapture;

    @Test
    public void shouldCreateOwnersForInvite() throws Exception {
        JsonNode inviteJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"instituteName\":\"gecasu\"}}");
        mockDefinitionManager();
        String testUserId = "be6d30e9-7c62-4a05-b4c8-ee28364da8e4";
        when(keycloakAdminUtil.createUser(any(), any(), any(), any(), any())).thenReturn(testUserId);
        when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
        when(shardManager.getShard(any())).thenReturn(new Shard());
        ReflectionTestUtils.setField(registryHelper, "workflowEnabled", true);
        registryHelper.inviteEntity(inviteJson, "");
        Mockito.verify(registryService).addEntity(shardCapture.capture(), userIdCapture.capture(), inputJsonCapture.capture(), anyBoolean());
        assertEquals("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"instituteName\":\"gecasu\",\"osOwner\":[\"" + testUserId + "\"]}}", inputJsonCapture.getValue().toString());
    }

    @Test
    public void shouldSendInviteInvitationsAfterCreatingOwners() throws Exception {
        JsonNode inviteJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"instituteName\":\"gecasu\"}}");
        mockDefinitionManager();
        String testUserId = "be6d30e9-7c62-4a05-b4c8-ee28364da8e4";
        when(keycloakAdminUtil.createUser(any(), any(), any(), any(), any())).thenReturn(testUserId);
        when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
        when(shardManager.getShard(any())).thenReturn(new Shard());
        registryHelper.inviteEntity(inviteJson, "");
        Mockito.verify(registryService).addEntity(shardCapture.capture(), userIdCapture.capture(), inputJsonCapture.capture(), anyBoolean());
        Mockito.verify(registryService, atLeastOnce()).callNotificationActors(operationCapture.capture(), toCapture.capture(), subjectCapture.capture(), messageCapture.capture());
        assertEquals("mailto:gecasu.ihises@tovinit.com", toCapture.getValue());
        assertEquals("INVITATION TO JOIN Institute", subjectCapture.getValue());
        assertEquals("You have been invited to join Institute registry. You can complete your profile here: https://ndear.xiv.in", messageCapture.getValue());
    }

    private void mockDefinitionManager() throws IOException {
        definitionsManager = new DefinitionsManager();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Definition> definitionMap = new HashMap<>();
        String studentSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
        String instituteSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Institute.json"), Charset.defaultCharset());
        definitionMap.put("Student", new Definition(objectMapper.readTree(studentSchema)));
        definitionMap.put("Institute", new Definition(objectMapper.readTree(instituteSchema)));
        ReflectionTestUtils.setField(definitionsManager, "definitionMap", definitionMap);
        ReflectionTestUtils.setField(registryHelper, "definitionsManager", definitionsManager);
        ReflectionTestUtils.setField(registryHelper.entityStateHelper, "definitionsManager", definitionsManager);
    }

    @Test
    public void shouldSendMultipleInviteInvitationsAfterCreatingOwners() throws Exception {
        JsonNode inviteJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"instituteName\":\"gecasu\",\"contactNumber\": \"123123\", \"adminEmail\": \"admin@email.com\",\n" +
                "  \"adminMobile\": \"1234\"\n" +
                "}}");
        String testUserId = "be6d30e9-7c62-4a05-b4c8-ee28364da8e4";
        when(keycloakAdminUtil.createUser(any(), any(), any(), any(), any())).thenReturn(testUserId);
        when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
        when(shardManager.getShard(any())).thenReturn(new Shard());
        mockDefinitionManager();
        registryHelper.inviteEntity(inviteJson, "");
        Mockito.verify(registryService).addEntity(shardCapture.capture(), userIdCapture.capture(), inputJsonCapture.capture(), anyBoolean());
        Mockito.verify(registryService, times(4)).callNotificationActors(operationCapture.capture(), toCapture.capture(), subjectCapture.capture(), messageCapture.capture());
        assertEquals("tel:123123", toCapture.getAllValues().get(0));
        assertEquals("INVITATION TO JOIN Institute", subjectCapture.getAllValues().get(0));
        assertEquals("You have been invited to join Institute registry. You can complete your profile here: https://ndear.xiv.in", messageCapture.getAllValues().get(0));
        assertEquals("mailto:gecasu.ihises@tovinit.com", toCapture.getAllValues().get(1));
        assertEquals("tel:1234", toCapture.getAllValues().get(2));
        assertEquals("mailto:admin@email.com", toCapture.getAllValues().get(3));
    }

    @Test
    public void shouldAbleToRemoveTheFormatAttributeFromDocumentObject() throws JsonProcessingException {
        JsonNode requestBody = new ObjectMapper().readTree("{\n" +
                "    \"program\": \"lol\",\n" +
                "    \"graduationYear\": \"2021\",\n" +
                "    \"marks\": \"78\",\n" +
                "    \"institute\": \"b62b3d52-cffe-428d-9dd1-61ba7b0a5882\",\n" +
                "    \"documents\": [\n" +
                "        {\n" +
                "            \"fileName\": \"e3266115-0bd0-4456-a347-96f4dc335761-blog_draft\",\n" +
                "            \"format\": \"file\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"fileName\": \"e56dab1b-bd92-41bb-b9e5-e991438f27b8-NDEAR.txt\",\n" +
                "            \"format\": \"file\"\n" +
                "        }\n" +
                "    ]\n" +
                "}");
        JsonNode expectedNode = new ObjectMapper().readTree("{\n" +
                "    \"program\": \"lol\",\n" +
                "    \"graduationYear\": \"2021\",\n" +
                "    \"marks\": \"78\",\n" +
                "    \"institute\": \"b62b3d52-cffe-428d-9dd1-61ba7b0a5882\",\n" +
                "    \"documents\": [\n" +
                "        {\n" +
                "            \"fileName\": \"e3266115-0bd0-4456-a347-96f4dc335761-blog_draft\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"fileName\": \"e56dab1b-bd92-41bb-b9e5-e991438f27b8-NDEAR.txt\"\n" +
                "        }\n" +
                "    ]\n" +
                "}");
        assertEquals(expectedNode,registryHelper.removeFormatAttr(requestBody));
    }

    @Test
    public void shouldAbleToInvalidateTheAttestation() throws Exception {
        String testInputJsonPath = getBaseDir() + "registryHelper/invalidateAttestation.json";
        String entity = "Student";
        String entityId = "1-aeb2498a-a7e5-487e-ac7d-5b271bb43a4f";
        JsonNode testInput = objectMapper.readTree(new File(testInputJsonPath));
        JsonNode inputNode = testInput.get("input");
        JsonNode expectedUpdatedNode = testInput.get("expected");

        when(shardManager.getShard(any())).thenReturn(new Shard());
        when(readService.getEntity(any(), any(), any(), any(), any())).thenReturn(inputNode);
        AttestationPolicy attestationPolicy1 = new AttestationPolicy();
        attestationPolicy1.setName("attestationEducationDetails");
        attestationPolicy1.setAttestationProperties(new HashMap<String, String>(){{
            put("name", "$.identityDetails.fullName");
            put("educationDetails", "$.educationDetails");
        }});
        AttestationPolicy attestationPolicy2 = new AttestationPolicy();
        attestationPolicy2.setName("attestationSomething");
        when(dbConnectionInfoMgr.getUuidPropertyName()).thenReturn("osid");
        definitionsManager = new DefinitionsManager();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Definition> definitionMap = new HashMap<>();
        String studentSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
        String instituteSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Institute.json"), Charset.defaultCharset());
        definitionMap.put("Student", new Definition(objectMapper.readTree(studentSchema)));
        definitionMap.put("Institute", new Definition(objectMapper.readTree(instituteSchema)));
        ReflectionTestUtils.setField(definitionsManager, "definitionMap", definitionMap);
        ReflectionTestUtils.setField(registryHelper, "definitionsManager", definitionsManager);
        registryHelper.invalidateAttestation(entity, entityId, "userId",null );
        verify(registryService, times(1)).updateEntity(any(), any(), any(), eq(expectedUpdatedNode.toString()));
    }
}