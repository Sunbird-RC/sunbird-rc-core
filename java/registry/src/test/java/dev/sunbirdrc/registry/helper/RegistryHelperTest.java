package dev.sunbirdrc.registry.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dev.sunbirdrc.keycloak.KeycloakAdminUtil;
import dev.sunbirdrc.pojos.AsyncRequest;
import dev.sunbirdrc.pojos.PluginResponseMessage;
import dev.sunbirdrc.pojos.SunbirdRCInstrumentation;
import dev.sunbirdrc.registry.entities.AttestationPolicy;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
import dev.sunbirdrc.registry.middleware.service.ConditionResolverService;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.service.*;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.sink.shard.ShardManager;
import dev.sunbirdrc.registry.util.*;
import dev.sunbirdrc.validators.IValidate;
import dev.sunbirdrc.validators.json.jsonschema.JsonValidationServiceImpl;
import dev.sunbirdrc.views.FunctionDefinition;
import dev.sunbirdrc.views.FunctionExecutor;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.sunbird.akka.core.SunbirdActorFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static dev.sunbirdrc.registry.Constants.ATTESTATION_POLICY;
import static dev.sunbirdrc.registry.Constants.REQUESTER;
import static dev.sunbirdrc.registry.Constants.REVOKED_CREDENTIAL;
import static dev.sunbirdrc.registry.middleware.util.Constants.FILTERS;
import static org.junit.Assert.*;
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
	@Spy
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
	@Qualifier("async")
	RegistryService registryAsyncService;

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
		assertEquals(jsonNode.get("Teacher").get(FILTERS).get("recordId").get("eq"), node.get("Teacher_Audit").get(0).get("recordId"));
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
		HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
		String uri = String.format("%s/%s/%s", entityName, entityId, propertyURI);
		when(httpServletRequest.getRequestURI()).thenReturn(uri);
		when(readService.getEntity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(getMockStudent());
		mockDefinitionManager();
		String propertyId = registryHelper.getPropertyIdAfterSavingTheProperty(entityName, entityId, requestBody, httpServletRequest);
		String actualPropertyId = "12345";
		Assert.assertEquals(propertyId, actualPropertyId);
	}

	@NotNull
	private ObjectNode getMockStudent() throws JsonProcessingException {
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
				"   \"osOwner\":\"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\",\n" +
				"    \"nextAttestationPolicy\": [{\n" +
				"      \"osid\":\"1-9f50f1b3-1234-4fcb-9e51-e0dbe0be19f9\"\n" +
				"    }]\n" +
				"}");
		student.set("Student", studentNodeContent);
		return student;
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

	@Mock
	AsyncRequest asyncRequest;

	@Test
	public void shouldCreateOwnersForInvite() throws Exception {
		JsonNode inviteJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"instituteName\":\"gecasu\"}}");
		mockDefinitionManager();
		String testUserId = "be6d30e9-7c62-4a05-b4c8-ee28364da8e4";
		when(keycloakAdminUtil.createUser(any(), any(), any(), any())).thenReturn(testUserId);
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
		when(keycloakAdminUtil.createUser(any(), any(), any(), any())).thenReturn(testUserId);
		when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		when(shardManager.getShard(any())).thenReturn(new Shard());
		ReflectionTestUtils.setField(registryHelper, "notificationEnabled", true);
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
		when(keycloakAdminUtil.createUser(any(), any(), any(), any())).thenReturn(testUserId);
		when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		when(shardManager.getShard(any())).thenReturn(new Shard());
		mockDefinitionManager();
		ReflectionTestUtils.setField(registryHelper, "notificationEnabled", true);
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
		assertEquals(expectedNode, registryHelper.removeFormatAttr(requestBody));
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
		attestationPolicy1.setAttestationProperties(new HashMap<String, String>() {{
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
		registryHelper.invalidateAttestation(entity, entityId, "userId", null);
		verify(registryService, times(1)).updateEntity(any(), any(), any(), eq(expectedUpdatedNode.toString()));
	}

	@Test
	public void shouldTriggerNextAttestationFlow() throws Exception {
		mockDefinitionManager();
		PluginResponseMessage pluginResponseMessage = PluginResponseMessage.builder()
				.policyName("test")
				.attestationOSID("test-1")
				.sourceEntity("Student")
				.policyName("testAttestationPolicy")
				.sourceOSID("1-b4907dc2-d3a8-49dc-a933-2b473bdd2ddb")
				.status("GRANT_CLAIM")
				.response("{}")
				.build();
		ObjectNode attestationPolicyObject = JsonNodeFactory.instance.objectNode();
		ArrayNode attestationArrayNodes = JsonNodeFactory.instance.arrayNode();
		ObjectNode mockAttestationPolicy = JsonNodeFactory.instance.objectNode();
		mockAttestationPolicy.set("onComplete", JsonNodeFactory.instance.textNode("attestation:nextAttestationPolicy"));
		mockAttestationPolicy.set("name", JsonNodeFactory.instance.textNode("testAttestationPolicy"));
		attestationArrayNodes.add(mockAttestationPolicy);
		ObjectNode mockAttestationPolicy2 = JsonNodeFactory.instance.objectNode();
		mockAttestationPolicy2.set("name", JsonNodeFactory.instance.textNode("nextAttestationPolicy"));
		mockAttestationPolicy2.set("attestorPlugin", JsonNodeFactory.instance.textNode("did:internal:ClaimPluginActor?entity=board-cbse"));
		attestationArrayNodes.add(mockAttestationPolicy2);
		attestationPolicyObject.set(ATTESTATION_POLICY, attestationArrayNodes);
		when(searchService.search(any())).thenReturn(attestationPolicyObject);
		when(readService.getEntity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(getMockStudent());
		registryHelper.entityStateHelper = mock(EntityStateHelper.class);
		when(registryHelper.entityStateHelper.manageState(any(), any(), any(), any(), any())).thenReturn(getMockStudent());
		when(dbConnectionInfoMgr.getUuidPropertyName()).thenReturn("osid");
		Config config = ConfigFactory.parseResources("sunbirdrc-actors.conf");
		SunbirdActorFactory sunbirdActorFactory = new SunbirdActorFactory(config, "dev.sunbirdrc.actors");
		sunbirdActorFactory.init("sunbirdrc-actors");
		registryHelper.updateState(pluginResponseMessage);
		verify(registryHelper, times(1)).triggerAttestation(any(), any());

	}

	@Test
	public void shouldNotTriggerNextAttestationFlowIfOnCompleteIsNotPresent() throws Exception {
		mockDefinitionManager();
		PluginResponseMessage pluginResponseMessage = PluginResponseMessage.builder()
				.policyName("test")
				.attestationOSID("test-1")
				.sourceEntity("Student")
				.policyName("testAttestationPolicy")
				.sourceOSID("1-b4907dc2-d3a8-49dc-a933-2b473bdd2ddb")
				.status("GRANT_CLAIM")
				.response("{}")
				.build();
		ObjectNode attestationPolicyObject = JsonNodeFactory.instance.objectNode();
		ArrayNode attestationArrayNodes = JsonNodeFactory.instance.arrayNode();
		ObjectNode mockAttestationPolicy = JsonNodeFactory.instance.objectNode();
		mockAttestationPolicy.set("name", JsonNodeFactory.instance.textNode("testAttestationPolicy"));
		attestationArrayNodes.add(mockAttestationPolicy);
		attestationPolicyObject.set(ATTESTATION_POLICY, attestationArrayNodes);
		when(searchService.search(any())).thenReturn(attestationPolicyObject);
		when(readService.getEntity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(getMockStudent());
		registryHelper.entityStateHelper = mock(EntityStateHelper.class);
		when(registryHelper.entityStateHelper.manageState(any(), any(), any(), any(), any())).thenReturn(getMockStudent());
		when(dbConnectionInfoMgr.getUuidPropertyName()).thenReturn("osid");
		Config config = ConfigFactory.parseResources("sunbirdrc-actors.conf");
		SunbirdActorFactory sunbirdActorFactory = new SunbirdActorFactory(config, "dev.sunbirdrc.actors");
		sunbirdActorFactory.init("sunbirdrc-actors");
		registryHelper.updateState(pluginResponseMessage);
		verify(registryHelper, times(0)).triggerAttestation(any(), any());

	}

	@Test
	public void shouldTriggerConcatFunctionOnAttestationCompleted() throws Exception {
		mockDefinitionManager();
		FunctionExecutor functionExecutorMock = Mockito.spy(FunctionExecutor.class);
		ReflectionTestUtils.setField(registryHelper, "functionExecutor", functionExecutorMock);
		definitionsManager.getDefinition("Student").getOsSchemaConfiguration().setFunctionDefinitions(Arrays.asList(
				FunctionDefinition.builder().name("concat").result("arg1 = arg2 + \" - \" + arg3").build()
		));
		PluginResponseMessage pluginResponseMessage = PluginResponseMessage.builder()
				.policyName("test")
				.attestationOSID("test-1")
				.sourceEntity("Student")
				.policyName("testAttestationPolicy")
				.sourceOSID("1-b4907dc2-d3a8-49dc-a933-2b473bdd2ddb")
				.status("GRANT_CLAIM")
				.response("{}")
				.build();
		ObjectNode attestationPolicyObject = JsonNodeFactory.instance.objectNode();
		ArrayNode attestationArrayNodes = JsonNodeFactory.instance.arrayNode();
		ObjectNode mockAttestationPolicy = JsonNodeFactory.instance.objectNode();
		mockAttestationPolicy.set("onComplete", JsonNodeFactory.instance.textNode("function:#/functionDefinitions/concat($.identityDetails.identityValue, $.identityDetails.gender, $.identityDetails.fullName)"));
		mockAttestationPolicy.set("name", JsonNodeFactory.instance.textNode("testAttestationPolicy"));
		attestationArrayNodes.add(mockAttestationPolicy);
		ObjectNode mockAttestationPolicy2 = JsonNodeFactory.instance.objectNode();
		mockAttestationPolicy2.set("name", JsonNodeFactory.instance.textNode("nextAttestationPolicy"));
		mockAttestationPolicy2.set("attestorPlugin", JsonNodeFactory.instance.textNode("did:internal:ClaimPluginActor?entity=board-cbse"));
		attestationArrayNodes.add(mockAttestationPolicy2);
		attestationPolicyObject.set(ATTESTATION_POLICY, attestationArrayNodes);
		when(searchService.search(any())).thenReturn(attestationPolicyObject);
		when(readService.getEntity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(getMockStudent());
		registryHelper.entityStateHelper = mock(EntityStateHelper.class);
		when(registryHelper.entityStateHelper.manageState(any(), any(), any(), any(), any())).thenReturn(getMockStudent());
		when(dbConnectionInfoMgr.getUuidPropertyName()).thenReturn("osid");
		Config config = ConfigFactory.parseResources("sunbirdrc-actors.conf");
		SunbirdActorFactory sunbirdActorFactory = new SunbirdActorFactory(config, "dev.sunbirdrc.actors");
		sunbirdActorFactory.init("sunbirdrc-actors");
		registryHelper.updateState(pluginResponseMessage);
		verify(functionExecutorMock, times(1)).execute(any(), any(), any());

	}

	@Test
	public void shouldTriggerProviderFunctionOnAttestationCompleted() throws Exception {
		mockDefinitionManager();
		FunctionExecutor functionExecutorMock = Mockito.spy(FunctionExecutor.class);
		ReflectionTestUtils.setField(registryHelper, "functionExecutor", functionExecutorMock);
		definitionsManager.getDefinition("Student").getOsSchemaConfiguration().setFunctionDefinitions(Arrays.asList(
				FunctionDefinition.builder().name("userDefinedConcat").provider("dev.sunbirdrc.provider.UUIDFunctionProvider").build()
		));
		PluginResponseMessage pluginResponseMessage = PluginResponseMessage.builder()
				.policyName("test")
				.attestationOSID("test-1")
				.sourceEntity("Student")
				.policyName("testAttestationPolicy")
				.sourceOSID("1-b4907dc2-d3a8-49dc-a933-2b473bdd2ddb")
				.status("GRANT_CLAIM")
				.response("{}")
				.build();
		ObjectNode attestationPolicyObject = JsonNodeFactory.instance.objectNode();
		ArrayNode attestationArrayNodes = JsonNodeFactory.instance.arrayNode();
		ObjectNode mockAttestationPolicy = JsonNodeFactory.instance.objectNode();
		mockAttestationPolicy.set("onComplete", JsonNodeFactory.instance.textNode("function:#/functionDefinitions/userDefinedConcat"));
		mockAttestationPolicy.set("name", JsonNodeFactory.instance.textNode("testAttestationPolicy"));
		attestationArrayNodes.add(mockAttestationPolicy);
		ObjectNode mockAttestationPolicy2 = JsonNodeFactory.instance.objectNode();
		mockAttestationPolicy2.set("name", JsonNodeFactory.instance.textNode("nextAttestationPolicy"));
		mockAttestationPolicy2.set("attestorPlugin", JsonNodeFactory.instance.textNode("did:internal:ClaimPluginActor?entity=board-cbse"));
		attestationArrayNodes.add(mockAttestationPolicy2);
		attestationPolicyObject.set(ATTESTATION_POLICY, attestationArrayNodes);
		when(searchService.search(any())).thenReturn(attestationPolicyObject);
		ObjectNode mockStudent = getMockStudent();
		when(readService.getEntity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(mockStudent);
		registryHelper.entityStateHelper = mock(EntityStateHelper.class);
		when(registryHelper.entityStateHelper.manageState(any(), any(), any(), any(), any())).thenReturn(mockStudent);
		when(dbConnectionInfoMgr.getUuidPropertyName()).thenReturn("osid");
		Config config = ConfigFactory.parseResources("sunbirdrc-actors.conf");
		SunbirdActorFactory sunbirdActorFactory = new SunbirdActorFactory(config, "dev.sunbirdrc.actors");
		sunbirdActorFactory.init("sunbirdrc-actors");
		registryHelper.updateState(pluginResponseMessage);
		verify(functionExecutorMock, times(1)).execute(any(), any(), any());

	}

	@Test
	public void shouldReturnTrueIfEntityContainsOwnershipAttributes() throws IOException {
		mockDefinitionManager();
		String entity = "Student";
		Assert.assertTrue(registryHelper.doesEntityOperationRequireAuthorization(entity));
	}

	@Test
	public void shouldReturnTrueIfEntityContainsManageRoles() throws IOException {
		mockDefinitionManager();
		definitionsManager.getDefinition("Student").getOsSchemaConfiguration().setRoles(Collections.singletonList("Admin"));
		String entity = "Student";
		Assert.assertTrue(registryHelper.doesEntityOperationRequireAuthorization(entity));
	}

	@Test
	public void shouldReturnFalseIfEntityDoesContainRolesAndOwnership() throws IOException {
		mockDefinitionManager();
		definitionsManager.getDefinition("Student").getOsSchemaConfiguration().setRoles(Collections.emptyList());
		definitionsManager.getDefinition("Student").getOsSchemaConfiguration().setOwnershipAttributes(Collections.emptyList());
		String entity = "Student";
		assertFalse(registryHelper.doesEntityOperationRequireAuthorization(entity));
	}

	@Test
	public void shouldDeleteReturnTrueIfEntityContainsOwnershipAttributes() throws IOException {
		mockDefinitionManager();
		String entity = "Student";
		Assert.assertTrue(registryHelper.doesEntityOperationRequireAuthorization(entity));
	}

	@Test
	public void shouldDeleteReturnTrueIfEntityContainsManageRoles() throws IOException {
		mockDefinitionManager();
		definitionsManager.getDefinition("Student").getOsSchemaConfiguration().setRoles(Collections.singletonList("Admin"));
		String entity = "Student";
		Assert.assertTrue(registryHelper.doesEntityOperationRequireAuthorization(entity));
	}

	@Test
	public void shouldDeleteReturnFalseIfEntityDoesContainRolesAndOwnership() throws IOException {
		mockDefinitionManager();
		definitionsManager.getDefinition("Student").getOsSchemaConfiguration().setRoles(Collections.emptyList());
		definitionsManager.getDefinition("Student").getOsSchemaConfiguration().setOwnershipAttributes(Collections.emptyList());
		String entity = "Student";
		assertFalse(registryHelper.doesEntityOperationRequireAuthorization(entity));
	}

	@Test
	public void shouldTriggerAsyncFlow() throws Exception {
		JsonNode inviteJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"instituteName\":\"gecasu\"}}");
		when(shardManager.getShard(any())).thenReturn(new Shard());

		when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		when(registryAsyncService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		when(asyncRequest.isEnabled()).thenReturn(Boolean.TRUE);
		String entity = registryHelper.addEntity(inviteJson, "");
		verify(registryService, never()).addEntity(any(), anyString(), any(), anyBoolean());
		verify(registryAsyncService, atLeastOnce()).addEntity(any(), anyString(), any(), anyBoolean());
	}

	@Test
	public void shouldRaiseClaimIfAttestationTypeIsAutomated() throws Exception {
		mockDefinitionManager();
		ObjectNode attestationPolicyObject = JsonNodeFactory.instance.objectNode();
		ArrayNode attestationArrayNodes = JsonNodeFactory.instance.arrayNode();
		ObjectNode mockAttestationPolicy = JsonNodeFactory.instance.objectNode();
		mockAttestationPolicy.set("name", JsonNodeFactory.instance.textNode("testAttestationPolicy"));
		mockAttestationPolicy.set("type", JsonNodeFactory.instance.textNode("AUTOMATED"));
		mockAttestationPolicy.set("attestorPlugin", JsonNodeFactory.instance.textNode("did:internal:ClaimPluginActor?entity=board-cbse"));
		ObjectNode mockAttestationProperties = JsonNodeFactory.instance.objectNode();
		mockAttestationProperties.set("fullName", JsonNodeFactory.instance.textNode("$.identityDetails.fullName"));
		mockAttestationProperties.set("gender", JsonNodeFactory.instance.textNode("$.identityDetails.gender"));
		mockAttestationPolicy.set("attestationProperties", mockAttestationProperties);
		attestationArrayNodes.add(mockAttestationPolicy);
		attestationPolicyObject.set(ATTESTATION_POLICY, attestationArrayNodes);
		JsonNode requestBody = new ObjectMapper().readTree("{\"Student\": {\n" +
				"    \"program\": \"Class C\",\n" +
				"    \"graduationYear\": \"2021\",\n" +
				"    \"marks\": \"78\",\n" +
				"	 \"osid\": \"12345\", \n" +
				"    \"institute\": \"b62b3d52-cffe-428d-9dd1-61ba7b0a5882\",\n" +
				"    \"documents\": [\n" +
				"        {\n" +
				"            \"fileName\": \"e3266115-0bd0-4456-a347-96f4dc335761-blog_draft\"\n" +
				"        },\n" +
				"        {\n" +
				"            \"fileName\": \"e56dab1b-bd92-41bb-b9e5-e991438f27b8-NDEAR.txt\"\n" +
				"        }\n" +
				"    ]\n" +
				"}\n}");
		when(searchService.search(any())).thenReturn(attestationPolicyObject);
		when(dbConnectionInfoMgr.getUuidPropertyName()).thenReturn("osid");
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
				"   \"osOwner\":\"556302c9-d8b4-4f60-9ac1-c16c8839a9f3\",\n" +
				"	\"testAttestationPolicy\": [{\n" +
				"	  \"osid\": \"1-7f50f1b3-1234-4fcb-1e51-e0dbe0be19f7\"" +
				"	}], \n" +
				"    \"nextAttestationPolicy\": [{\n" +
				"      \"osid\":\"1-9f50f1b3-1234-4fcb-9e51-e0dbe0be19f9\"\n" +
				"    }]\n" +
				"}");
		student.set("Student", studentNodeContent);
		when(readService.getEntity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(student);
		Config config = ConfigFactory.parseResources("sunbirdrc-actors.conf");
		SunbirdActorFactory sunbirdActorFactory = new SunbirdActorFactory(config, "dev.sunbirdrc.actors");
		sunbirdActorFactory.init("sunbirdrc-actors");
		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		objectNode.set("fullName", JsonNodeFactory.instance.textNode("First Avenger"));
		objectNode.set("gender", JsonNodeFactory.instance.textNode("Male"));
		ReflectionTestUtils.setField(registryHelper, "workflowEnabled", true);
		registryHelper.autoRaiseClaim("Student", "12345", "556302c9-d8b4-4f60-9ac1-c16c8839a9f3", null, requestBody, "");
		verify(conditionResolverService, times(1)).resolve(objectNode, REQUESTER, null, Collections.emptyList());
		verify(registryHelper, times(1)).triggerAttestation(any(), any());
	}

	public void shouldStoredSignedDataInRevokedCredentialsRegistry() throws Exception {
		when(shardManager.getShard(any())).thenReturn(new Shard());
		when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		registryHelper.revokeExistingCredentials("Student", "student-osid", "userId", "signed-data");
		verify(registryService, atLeastOnce()).addEntity(any(), anyString(), any(), anyBoolean());
	}

	@Test
	public void shouldNotStoredSignedDataIfNullOrEmptyInRevokedCredentialsRegistry() throws Exception {
		when(shardManager.getShard(any())).thenReturn(new Shard());
		when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		registryHelper.revokeExistingCredentials("Student", "student-osid", "userId", "");
		verify(registryService, never()).addEntity(any(), anyString(), any(), anyBoolean());
		registryHelper.revokeExistingCredentials("Student", "student-osid", "userId", null);
		verify(registryService, never()).addEntity(any(), anyString(), any(), anyBoolean());
	}

	@Test
	public void shouldReturnTrueIFSignedDataIsRevoked() throws Exception {
		JsonNode searchResponse = JsonNodeFactory.instance.objectNode().set(REVOKED_CREDENTIAL, JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("signedData", "xyz")));
		when(searchService.search(any())).thenReturn(searchResponse);
		assertTrue(registryHelper.checkIfCredentialIsRevoked("signedData"));
	}

	@Test
	public void shouldReturnFalseIfSignedDataIsNotRevoked() throws Exception {
		JsonNode searchResponse = JsonNodeFactory.instance.objectNode().set(REVOKED_CREDENTIAL, JsonNodeFactory.instance.arrayNode());
		when(searchService.search(any())).thenReturn(searchResponse);
		assertFalse(registryHelper.checkIfCredentialIsRevoked("signedData"));
	}

	@Test
	public void shouldNotContainShardIdInAsyncMode() throws Exception {
		JsonNode inviteJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"instituteName\":\"gecasu\"}}");
		Shard shard = mock(Shard.class);
		when(shard.getShardLabel()).thenReturn("1");
		when(shardManager.getShard(any())).thenReturn(shard);

		when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		when(registryAsyncService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		when(asyncRequest.isEnabled()).thenReturn(Boolean.TRUE);
		String entity = registryHelper.addEntity(inviteJson, "");
		verify(registryService, never()).addEntity(any(), anyString(), any(), anyBoolean());
		verify(registryAsyncService, atLeastOnce()).addEntity(any(), anyString(), any(), anyBoolean());
		assertFalse(entity.startsWith("1-"));
	}

	@Test
	public void shouldContainShardIdInSyncMode() throws Exception {
		JsonNode inviteJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"instituteName\":\"gecasu\"}}");
		Shard shard = mock(Shard.class);
		when(shard.getShardLabel()).thenReturn("1");
		when(shardManager.getShard(any())).thenReturn(shard);

		when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		when(registryAsyncService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		when(asyncRequest.isEnabled()).thenReturn(Boolean.FALSE);
		String entity = registryHelper.addEntity(inviteJson, "");
		verify(registryService, atLeastOnce()).addEntity(any(), anyString(), any(), anyBoolean());
		verify(registryAsyncService, never()).addEntity(any(), anyString(), any(), anyBoolean());
		assertTrue(entity.startsWith("1-"));
	}

	void mockValidationService() throws IOException {
		String studentSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		String instituteSchema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Institute.json"), Charset.defaultCharset());

		IValidate jsonValidationService = new JsonValidationServiceImpl("");
		jsonValidationService.addDefinitions("Student", studentSchema);
		jsonValidationService.addDefinitions("Institute", instituteSchema);
		ReflectionTestUtils.setField(registryHelper, "validationService", jsonValidationService);
	}

	@Test
	public void shouldRaiseRequiredExceptions() throws Exception {
		JsonNode inviteJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\"}}");
		mockDefinitionManager();
		mockValidationService();
		String testUserId = "be6d30e9-7c62-4a05-b4c8-ee28364da8e4";
		when(keycloakAdminUtil.createUser(any(), any(), any(), any())).thenReturn(testUserId);
		when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		when(shardManager.getShard(any())).thenReturn(new Shard());
		ReflectionTestUtils.setField(registryHelper, "workflowEnabled", true);
		ReflectionTestUtils.setField(registryHelper, "skipRequiredValidationForInvite", false);
		try {
			registryHelper.inviteEntity(inviteJson, "");
		} catch (MiddlewareHaltException e) {
			assertEquals("Validation Exception : #/Institute: required key [instituteName] not found", e.getMessage());
		}
	}

	@Test
	public void shouldNotRaiseRequiredExceptionsIFFlagDisabled() throws Exception {
		JsonNode inviteJson = new ObjectMapper().readTree("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\"}}");
		mockDefinitionManager();
		mockValidationService();
		String testUserId = "be6d30e9-7c62-4a05-b4c8-ee28364da8e4";
		when(keycloakAdminUtil.createUser(any(), any(), any(), any())).thenReturn(testUserId);
		when(registryService.addEntity(any(), any(), any(), anyBoolean())).thenReturn(UUID.randomUUID().toString());
		when(shardManager.getShard(any())).thenReturn(new Shard());
		ReflectionTestUtils.setField(registryHelper, "workflowEnabled", true);
		ReflectionTestUtils.setField(registryHelper, "skipRequiredValidationForInvite", true);
		registryHelper.inviteEntity(inviteJson, "");
		Mockito.verify(registryService).addEntity(shardCapture.capture(), userIdCapture.capture(), inputJsonCapture.capture(), anyBoolean());
		assertEquals("{\"Institute\":{\"email\":\"gecasu.ihises@tovinit.com\",\"osOwner\":[\"" + testUserId + "\"]}}", inputJsonCapture.getValue().toString());
	}
}
