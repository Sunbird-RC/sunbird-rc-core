package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.sunbirdrc.pojos.HealthCheckResponse;
import dev.sunbirdrc.registry.dao.IRegistryDao;
import dev.sunbirdrc.registry.entities.SchemaStatus;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.DBConnectionInfo;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.service.SchemaService;
import dev.sunbirdrc.registry.sink.DBProviderFactory;
import dev.sunbirdrc.registry.sink.DatabaseProvider;
import dev.sunbirdrc.registry.sink.OSGraph;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.sink.shard.ShardManager;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import dev.sunbirdrc.registry.util.OSSystemFieldsHelper;
import dev.sunbirdrc.validators.IValidate;
import dev.sunbirdrc.validators.json.jsonschema.JsonValidationServiceImpl;
import org.apache.commons.io.IOUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;

import static dev.sunbirdrc.registry.Constants.Schema;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DefinitionsManager.class, ObjectMapper.class, DBProviderFactory.class, DBConnectionInfoMgr.class, DBConnectionInfo.class})
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
	private RegistryServiceImpl registryService;

	@Mock
	private ShardManager shardManager;

	@Mock
	private IValidate validator;

	@Mock
	private DBConnectionInfoMgr dbConnectionInfoMgr;

	@Mock
	private RestTemplate mockRestTemplate;
	@Mock
	private EncryptionServiceImpl encryptionService;
	@Mock
	private SignatureServiceImpl signatureService;
	@Mock
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
	}

	@Before
	public void initialize() throws IOException {
		dbConnectionInfoMgr.setUuidPropertyName("tid");

		mockDatabaseProvider = dbProviderFactory.getInstance(null);
		try (OSGraph osGraph = mockDatabaseProvider.getOSGraph()) {
			graph = osGraph.getGraphStore();
		} catch (Exception e) {
		}
		setup();
	}

	@Test
	public void test_health_check_up_scenario() throws Exception {
		when(encryptionService.isEncryptionServiceUp()).thenReturn(true);
		when(mockDatabaseProvider.isDatabaseServiceUp()).thenReturn(true);
		when(shard.getDatabaseProvider()).thenReturn(mockDatabaseProvider);
		when(shardManager.getDefaultShard()).thenReturn(shard);
		when(signatureService.isServiceUp()).thenReturn(true);
		HealthCheckResponse response = registryServiceForHealth.health(shardManager.getDefaultShard());
		assertTrue(response.isHealthy());
		response.getChecks().forEach(ch -> assertTrue(ch.isHealthy()));
	}

	@Test
	public void test_health_check_down_scenario() throws Exception {
		when(signatureService.isServiceUp()).thenReturn(true);
		when(encryptionService.isEncryptionServiceUp()).thenReturn(false);
		when(mockDatabaseProvider.isDatabaseServiceUp()).thenReturn(true);
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

}
