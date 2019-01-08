package io.opensaber.registry.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.app.OpenSaberApplication;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryController;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.AuditRecord;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.util.ReadConfigurator;

import java.io.IOException;

import io.opensaber.registry.util.RecordIdentifier;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { OpenSaberApplication.class, RegistryController.class, GenericConfiguration.class,
		EncryptionServiceImpl.class, RegistryDaoImpl.class, AuditRecord.class, SignatureServiceImpl.class })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryServiceImplTest extends RegistryTestBase {

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

	@Mock
	private RestTemplate mockRestTemplate;
	@Mock
	private EncryptionServiceImpl encryptionService;
	@Mock
	private SignatureServiceImpl signatureService;
	private DatabaseProvider mockDatabaseProvider;
	@Mock
	private IRegistryDao registryDao;
	@InjectMocks
	private RegistryServiceImpl registryServiceForHealth;


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
		setup();
	}

	@Test
	public void test_health_check_up_scenario() throws Exception {
		when(encryptionService.isEncryptionServiceUp()).thenReturn(true);
		when(mockDatabaseProvider.isDatabaseServiceUp()).thenReturn(true);
		when(signatureService.isServiceUp()).thenReturn(true);
		HealthCheckResponse response = registryServiceForHealth.health();
		assertTrue(response.isHealthy());
		response.getChecks().forEach(ch -> assertTrue(ch.isHealthy()));
	}

	@Test
	public void test_health_check_down_scenario() throws Exception {
		when(encryptionService.isEncryptionServiceUp()).thenReturn(true);
		when(mockDatabaseProvider.isDatabaseServiceUp()).thenReturn(false);
		when(signatureService.isServiceUp()).thenReturn(true);
		HealthCheckResponse response = registryServiceForHealth.health();
		System.out.println(response.toString());

		assertFalse(response.isHealthy());
		response.getChecks().forEach(ch -> {
			if (ch.getName().equalsIgnoreCase(Constants.SUNBIRD_ENCRYPTION_SERVICE_NAME)) {
				assertTrue(ch.isHealthy());
			}
			if (ch.getName().equalsIgnoreCase(Constants.SUNBIRD_SIGNATURE_SERVICE_NAME)) {
				assertTrue(ch.isHealthy());
			} else {
				assertFalse(ch.isHealthy());
			}
		});
	}

	@Ignore
	@Test
	public void test_update_parent_entity_after_creating() throws Exception {

		String validJsonString = getValidJsonString(VALID_TEST_INPUT_JSON);
		shardManager.activateShard("");
		ReadConfigurator configurator = new ReadConfigurator();
		String resultId = registryService.addEntity(validJsonString);
		RecordIdentifier recordIdentifier = RecordIdentifier.parse(resultId);
		String updatedInput = getValidStringForUpdate(resultId);
		registryService.updateEntity(recordIdentifier.getUuid(), updatedInput);
		JsonNode readJson = registryService.getEntity(resultId,configurator);
		JsonNode updateInputJson = new ObjectMapper().readTree(updatedInput);
		assertEquals(readJson.get("gender"),updateInputJson.get("Teacher").get("gender"));
		System.out.println("graph::::"+readJson.toString());
		closeDB();
	}

	public void closeDB() throws Exception {
		databaseProvider.shutdown();
	}

}
