package io.opensaber.registry.service.impl;

import io.opensaber.pojos.AuditRecord;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.app.OpenSaberApplication;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryController;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.sink.shard.ShardManager;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.FieldSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { OpenSaberApplication.class, RegistryController.class, GenericConfiguration.class,
		EncryptionServiceImpl.class, AuditRecord.class, SignatureServiceImpl.class,
		DBProviderFactory.class, DBConnectionInfoMgr.class, DBConnectionInfo.class, RegistryServiceImpl.class })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryServiceImplTest {

	private static final String VALID_JSONLD = "school.jsonld";
	private static final String VALIDNEW_JSONLD = "school1.jsonld";
	private static final String CONTEXT_CONSTANT = "sample:";
	private static final String VALID_TEST_INPUT_JSON = "teacher-valid.json";
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	private boolean isInitialized = false;
	@Value("${registry.context.base}")
	private String registryContextBase;
	@Autowired
	private RegistryServiceImpl registryService;

	@Autowired
	private ShardManager shardManager;

	@Autowired
	private DBConnectionInfoMgr dbConnectionInfoMgr;

	@Mock
	private RestTemplate mockRestTemplate;
	@Mock
	private EncryptionServiceImpl encryptionService;
	@Mock
	private SignatureServiceImpl signatureService;
	private DatabaseProvider mockDatabaseProvider;

	private IRegistryDao registryDao;
	@InjectMocks
	private RegistryServiceImpl registryServiceForHealth;

	@Autowired
	private DBProviderFactory dbProviderFactory;

	private Graph graph;

	public void setup() {
		if (!isInitialized) {
			MockitoAnnotations.initMocks(this);
			ReflectionTestUtils.setField(encryptionService, "encryptionServiceHealthCheckUri", "encHealthCheckUri");
			ReflectionTestUtils.setField(encryptionService, "decryptionUri", "decryptionUri");
			ReflectionTestUtils.setField(encryptionService, "encryptionUri", "encryptionUri");
			ReflectionTestUtils.setField(signatureService, "healthCheckURL", "healthCheckURL");
			isInitialized = true;
		}
	}

	@Before
	public void initialize() throws IOException {
		dbConnectionInfoMgr.setUuidPropertyName("tid");

		mockDatabaseProvider = dbProviderFactory.getInstance(null);
		try (OSGraph osGraph = mockDatabaseProvider.getOSGraph()) {
			graph = osGraph.getGraphStore();
		} catch (Exception e) {}
		setup();
	}

	@Test
	public void test_health_check_up_scenario() throws Exception {
		when(encryptionService.isEncryptionServiceUp()).thenReturn(true);
		//when(mockDatabaseProvider.isDatabaseServiceUp()).thenReturn(true);
		when(signatureService.isServiceUp()).thenReturn(true);
		HealthCheckResponse response = registryServiceForHealth.health(shardManager.getDefaultShard());
		assertTrue(response.isHealthy());
		response.getChecks().forEach(ch -> assertTrue(ch.isHealthy()));
	}

	@Test
	public void test_health_check_down_scenario() throws Exception {
		when(signatureService.isServiceUp()).thenReturn(true);
		when(encryptionService.isEncryptionServiceUp()).thenReturn(false);
		FieldSetter.setField(registryServiceForHealth, registryServiceForHealth.getClass().
				getDeclaredField("encryptionEnabled"), true);
		FieldSetter.setField(registryServiceForHealth, registryServiceForHealth.getClass().
				getDeclaredField("signatureEnabled"), true);

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
}
