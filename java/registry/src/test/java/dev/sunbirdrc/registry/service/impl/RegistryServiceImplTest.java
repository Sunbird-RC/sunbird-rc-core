package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthCheckResponse;
import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.registry.dao.IRegistryDao;
import dev.sunbirdrc.registry.dao.VertexReader;
import dev.sunbirdrc.registry.dao.VertexWriter;
import dev.sunbirdrc.registry.entities.SchemaStatus;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.model.DBConnectionInfo;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.service.IAuditService;
import dev.sunbirdrc.registry.service.SchemaService;
import dev.sunbirdrc.registry.sink.DBProviderFactory;
import dev.sunbirdrc.registry.sink.DatabaseProvider;
import dev.sunbirdrc.registry.sink.OSGraph;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.sink.shard.ShardManager;
import dev.sunbirdrc.registry.util.*;
import dev.sunbirdrc.validators.IValidate;
import dev.sunbirdrc.validators.json.jsonschema.JsonValidationServiceImpl;
import org.apache.commons.io.IOUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.sunbird.akka.core.SunbirdActorFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

import static dev.sunbirdrc.registry.Constants.Schema;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DefinitionsManager.class, ObjectMapper.class, DBProviderFactory.class, DBConnectionInfoMgr.class, DBConnectionInfo.class,  OSResourceLoader.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryServiceImplTest {
	@Value("${registry.schema.url}")
	private String schemaUrl;
	private String validationType = "json";

	public Constants.SchemaType getValidationType() throws IllegalArgumentException {
		String validationMechanism = validationType.toUpperCase();
		Constants.SchemaType st = Constants.SchemaType.valueOf(validationMechanism);

		return st;
	}

	private static final String TRAINING_CERTIFICATE = "TrainingCertificate";
	private static final String VALID_JSONLD = "school.jsonld";
	private static final String VALIDNEW_JSONLD = "school1.jsonld";
	private static final String CONTEXT_CONSTANT = "sample:";
	private static final String VALID_TEST_INPUT_JSON = "teacher-valid.json";
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	private boolean isInitialized = false;
	@Value("${registry.context.base}")
	private String registryContextBase;
	@InjectMocks
	@Qualifier("sync")
	@Spy
	private RegistryServiceImpl registryService;

	@Mock
	private ShardManager shardManager;

	@Mock
	private IValidate validator;

	@Autowired
	private DBConnectionInfoMgr dbConnectionInfoMgr;

	@Mock
	private RestTemplate mockRestTemplate;
	@Mock
	private EncryptionServiceImpl encryptionService;
	@Mock
	private SignatureServiceImpl signatureService;

	@Mock
	private HealthIndicator healthIndicator;

	private DatabaseProvider mockDatabaseProvider;

	private IRegistryDao registryDao;
	@InjectMocks
	private RegistryServiceImpl registryServiceForHealth;

	@Autowired
	private DBProviderFactory dbProviderFactory;

	@Autowired
	private ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private OSSystemFieldsHelper systemFieldsHelper;


	private Graph graph;

	@Autowired
	private DefinitionsManager definitionsManager;

	@Mock
	private Shard shard;

	private final SchemaService schemaService = new SchemaService();

	@Mock
	private JsonValidationServiceImpl jsonValidationService;
	@Mock
	private IAuditService auditService;

	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(encryptionService, "encryptionServiceHealthCheckUri", "encHealthCheckUri");
		ReflectionTestUtils.setField(encryptionService, "decryptionUri", "decryptionUri");
		ReflectionTestUtils.setField(encryptionService, "encryptionUri", "encryptionUri");
		ReflectionTestUtils.setField(signatureService, "healthCheckURL", "healthCheckURL");
		ReflectionTestUtils.setField(registryService, "definitionsManager", definitionsManager);
		ReflectionTestUtils.setField(schemaService, "definitionsManager", definitionsManager);
		ReflectionTestUtils.setField(schemaService, "validator", jsonValidationService);
		ReflectionTestUtils.setField(registryService, "schemaService", schemaService);
		ReflectionTestUtils.setField(registryService, "objectMapper", objectMapper);
	}

	@Before
	public void initialize() throws IOException {
		Config config = ConfigFactory.parseResources("sunbirdrc-actors.conf");

		SunbirdActorFactory sunbirdActorFactory = new SunbirdActorFactory(config, "dev.sunbirdrc.actors");
		sunbirdActorFactory.init("sunbirdrc-actors");
		dbConnectionInfoMgr.setUuidPropertyName("osid");
		mockDatabaseProvider = dbProviderFactory.getInstance(null);
		graph = mockDatabaseProvider.getOSGraph().getGraphStore();
		populateGraph();
		setup();
	}

	private void populateGraph() {
		VertexWriter vertexWriter = new VertexWriter(graph, mockDatabaseProvider, "osid");
		Vertex v1 = vertexWriter.createVertex("Teacher");
		v1.property("serialNum", 1);
		v1.property("teacherName", "marko");
		Vertex v2 = vertexWriter.createVertex("Teacher");
		v2.property("serialNum", 2);
		v2.property("teacherName", "vedas");
		Vertex v3 = vertexWriter.createVertex("Teacher");
		v3.property("serialNum", 3);
		v3.property("teacherName", "jas");

	}



	@Test
	public void test_health_check_up_scenario() throws Exception {
		when(encryptionService.getHealthInfo()).thenReturn(new ComponentHealthInfo(Constants.SUNBIRD_ENCRYPTION_SERVICE_NAME, true));
		mockDatabaseProvider = mock(DatabaseProvider.class);
		when(mockDatabaseProvider.getHealthInfo()).thenReturn(new ComponentHealthInfo(Constants.SUNBIRDRC_DATABASE_NAME, true));
		ReflectionTestUtils.setField(registryServiceForHealth, "healthIndicators", Arrays.asList(encryptionService, mockDatabaseProvider));
		when(shard.getDatabaseProvider()).thenReturn(mockDatabaseProvider);
		when(shardManager.getDefaultShard()).thenReturn(shard);
		when(signatureService.isServiceUp()).thenReturn(true);
		HealthCheckResponse response = registryServiceForHealth.health(shardManager.getDefaultShard());
		assertTrue(response.isHealthy());
		response.getChecks().forEach(ch -> assertTrue(ch.isHealthy()));
	}

	@Test
	public void test_health_check_down_scenario() throws Exception {
		mockDatabaseProvider = mock(DatabaseProvider.class);
		when(signatureService.getHealthInfo()).thenReturn(new ComponentHealthInfo(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME, true));
		when(encryptionService.getHealthInfo()).thenReturn(new ComponentHealthInfo(Constants.SUNBIRD_ENCRYPTION_SERVICE_NAME, false));
		when(mockDatabaseProvider.getHealthInfo()).thenReturn(new ComponentHealthInfo(Constants.SUNBIRDRC_DATABASE_NAME, true));
		ReflectionTestUtils.setField(registryServiceForHealth, "healthIndicators", Arrays.asList(signatureService, encryptionService, mockDatabaseProvider));
		when(shard.getDatabaseProvider()).thenReturn(mockDatabaseProvider);
		when(shardManager.getDefaultShard()).thenReturn(shard);
		when(signatureService.isServiceUp()).thenReturn(true);
		ReflectionTestUtils.setField(registryServiceForHealth, "encryptionEnabled", true);
		ReflectionTestUtils.setField(registryServiceForHealth, "signatureEnabled", true);

		HealthCheckResponse response = registryServiceForHealth.health(shardManager.getDefaultShard());
		System.out.println(response.toString());

		assertFalse(response.isHealthy());
		response.getChecks().forEach(ch -> {
			if (ch.getName().equalsIgnoreCase(Constants.SUNBIRD_ENCRYPTION_SERVICE_NAME)) {
				assertFalse(ch.isHealthy());
			} else if (ch.getName().equalsIgnoreCase(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME)) {
				assertTrue(ch.isHealthy());
			} else {
				assertTrue(ch.isHealthy());
			}
		});
	}

	@Test
	public void shouldAddSchemaToDefinitionManager() throws Exception {
		assertEquals(2, definitionsManager.getAllKnownDefinitions().size());
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
		ObjectNode object = JsonNodeFactory.instance.objectNode();
		object.put(Schema.toLowerCase(), schema);
		object.put("status", SchemaStatus.PUBLISHED.toString());
		schemaNode.set(Schema, object);
		registryService.addEntity(shard, "", schemaNode, true);
		assertEquals(3, definitionsManager.getAllKnownDefinitions().size());
	}

	@Test
	public void shouldNotAddSchemaToDefinitionManagerForDraftStatus() throws Exception {
		assertEquals(2, definitionsManager.getAllKnownDefinitions().size());
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
		ObjectNode object = JsonNodeFactory.instance.objectNode();
		object.put(Schema.toLowerCase(), schema);
		schemaNode.set(Schema, object);
		assertNull(schemaNode.get("status"));
		registryService.addEntity(shard, "", schemaNode, true);
		assertNotNull(schemaNode.get(Schema).get("status"));
		assertEquals(2, definitionsManager.getAllKnownDefinitions().size());
	}

	@Test
	public void shouldStoreOnlyPublicFieldsInES() throws Exception {
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		definitionsManager.appendNewDefinition(JsonNodeFactory.instance.textNode(schema));
		ReflectionTestUtils.setField(registryService, "persistenceEnabled", true);
		ReflectionTestUtils.setField(registryService, "uuidPropertyName", "osid");
		ReflectionTestUtils.setField(registryService, "searchProvider", "dev.sunbirdrc.registry.service.ElasticSearchService");
		when(shard.getDatabaseProvider()).thenReturn(mockDatabaseProvider);
		ObjectNode inputJson = JsonNodeFactory.instance.objectNode();
		inputJson.set("Student", objectMapper.readTree("{\n" +
				"  \"name\": \"t\",\n" +
				"  \"identityDetails\": {\n" +
				"    \"dob\": \"10-10-1995\"\n" +
				"  },\n" +
				"  \"contactDetails\": {\n" +
				"    \"email\": \"test@mail.com\"\n" +
				"  }\n" +
				"}"));
		registryService.addEntity(shard, "", inputJson, true);
		ArgumentCaptor<JsonNode> esNodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
		verify(registryService, times(1)).callESActors(esNodeCaptor.capture(),any(),any(),any(),any());
		esNodeCaptor.getValue();
		System.out.println(esNodeCaptor);
		JsonNode output = esNodeCaptor.getValue().get("Student");
		assertFalse(output.get("identityDetails").has("dob"));
		assertFalse(output.get("contactDetails").has("email"));
		assertTrue(output.has("name"));
		definitionsManager.removeDefinition(JsonNodeFactory.instance.textNode(schema));
	}

	@Test
	public void shouldNotRemoveAnyFieldsInAdd() throws Exception {
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Teacher.json"), Charset.defaultCharset());
		definitionsManager.appendNewDefinition(JsonNodeFactory.instance.textNode(schema));
		ReflectionTestUtils.setField(registryService, "persistenceEnabled", true);
		ReflectionTestUtils.setField(registryService, "uuidPropertyName", "osid");
		ReflectionTestUtils.setField(registryService, "searchProvider", "dev.sunbirdrc.registry.service.ElasticSearchService");
		when(shard.getDatabaseProvider()).thenReturn(mockDatabaseProvider);
		ObjectNode inputJson = JsonNodeFactory.instance.objectNode();
		inputJson.set("Teacher", objectMapper.readTree("{\n" +
				"  \"fullName\": \"abc\",\n" +
				"  \"gender\": \"male\",\n" +
				"  \"dob\": \"10-10-1995\"\n" +
				"}"));
		registryService.addEntity(shard, "", inputJson, true);
		ArgumentCaptor<JsonNode> esNodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
		verify(registryService, times(1)).callESActors(esNodeCaptor.capture(),any(),any(),any(),any());
		esNodeCaptor.getValue();
		System.out.println(esNodeCaptor);
		JsonNode output = esNodeCaptor.getValue().get("Teacher");
		assertTrue(output.has("dob"));
		assertTrue(output.has("gender"));
		assertTrue(output.has("fullName"));
		definitionsManager.removeDefinition(JsonNodeFactory.instance.textNode(schema));
	}

	@Test
	public void shouldUpdateOnlyPublicFieldsInES() throws Exception {
		
		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Student.json"), Charset.defaultCharset());
		definitionsManager.appendNewDefinition(JsonNodeFactory.instance.textNode(schema));
		ReflectionTestUtils.setField(registryService, "persistenceEnabled", true);
		ReflectionTestUtils.setField(registryService, "uuidPropertyName", "osid");
		ReflectionTestUtils.setField(registryService, "searchProvider", "dev.sunbirdrc.registry.service.ElasticSearchService");
		when(shard.getDatabaseProvider()).thenReturn(mockDatabaseProvider);
		ObjectNode inputJson = JsonNodeFactory.instance.objectNode();
		String studentOsid = addStudentToGraph();
		inputJson.set("Student", objectMapper.readTree("{\n" +
				"  \"osid\": \"" + studentOsid + "\"," +
				"  \"name\": \"t\",\n" +
				"  \"identityDetails\": {\n" +
				"    \"dob\": \"10-10-1995\"\n" +
				"  },\n" +
				"  \"contactDetails\": {\n" +
				"    \"email\": \"test@mail.com\"\n" +
				"  }\n" +
				"}"));
		when(shard.getShardLabel()).thenReturn("");
		registryService.updateEntity(shard, "", studentOsid, String.valueOf(inputJson));
		ArgumentCaptor<JsonNode> esNodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
		verify(registryService, times(1)).callESActors(esNodeCaptor.capture(),any(),any(),any(),any());
		esNodeCaptor.getValue();
		System.out.println(esNodeCaptor);
		JsonNode output = esNodeCaptor.getValue().get("Student");
		assertFalse(output.get("identityDetails").has("dob"));
		assertFalse(output.get("contactDetails").has("email"));
		assertTrue(output.has("name"));
		assertEquals("t", output.get("name").asText());
		definitionsManager.removeDefinition(JsonNodeFactory.instance.textNode(schema));
	}

	@Test
	public void shouldNotRemoveAnyFieldsInUpdate() throws Exception {

		String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("Teacher.json"), Charset.defaultCharset());
		definitionsManager.appendNewDefinition(JsonNodeFactory.instance.textNode(schema));
		ReflectionTestUtils.setField(registryService, "persistenceEnabled", true);
		ReflectionTestUtils.setField(registryService, "uuidPropertyName", "osid");
		ReflectionTestUtils.setField(registryService, "searchProvider", "dev.sunbirdrc.registry.service.ElasticSearchService");
		when(shard.getDatabaseProvider()).thenReturn(mockDatabaseProvider);
		ObjectNode inputJson = JsonNodeFactory.instance.objectNode();
		String studentOsid = addTeacherToGraph();
		inputJson.set("Teacher", objectMapper.readTree("{\n" +
				"  \"osid\": \"" + studentOsid + "\"," +
				"  \"gender\": \"male\",\n" +
				"  \"dob\": \"10-10-1995\"\n" +
				"}"));
		when(shard.getShardLabel()).thenReturn("");
		registryService.updateEntity(shard, "", studentOsid, String.valueOf(inputJson));
		ArgumentCaptor<JsonNode> esNodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
		verify(registryService, times(1)).callESActors(esNodeCaptor.capture(),any(),any(),any(),any());
		esNodeCaptor.getValue();
		System.out.println(esNodeCaptor);
		JsonNode output = esNodeCaptor.getValue().get("Teacher");
		assertTrue(output.has("dob"));
		assertTrue(output.has("gender"));
		assertTrue(output.has("fullName"));
		definitionsManager.removeDefinition(JsonNodeFactory.instance.textNode(schema));
	}

	@Test
	public void shouldTestVertexWriter() throws Exception {
		String v1 = addStudentToGraph();
		ReadConfigurator readConfigurator = ReadConfiguratorFactory.getForUpdateValidation();
		VertexReader vertexReader = new VertexReader(mockDatabaseProvider, graph, readConfigurator, "osid", definitionsManager);
		JsonNode student = vertexReader.read("Student", v1);
		assertNotNull(student);
	}

	private String addStudentToGraph() throws JsonProcessingException {
		VertexWriter vertexWriter = new VertexWriter(graph, mockDatabaseProvider, "osid");
		return vertexWriter.writeNodeEntity(objectMapper.readTree("{\"Student\":  {\n" +
				"  \"name\": \"abc\"\n" +
				"}}"));
	}

	private String addTeacherToGraph() throws JsonProcessingException {
		VertexWriter vertexWriter = new VertexWriter(graph, mockDatabaseProvider, "osid");
		return vertexWriter.writeNodeEntity(objectMapper.readTree("{\"Teacher\":  {\n" +
				"  \"fullName\": \"abc\"\n" +
				"}}"));
	}
}
