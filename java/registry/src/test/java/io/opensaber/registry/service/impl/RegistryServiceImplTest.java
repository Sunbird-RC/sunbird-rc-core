package io.opensaber.registry.service.impl;

import static org.junit.Assert.*;

import io.opensaber.converters.JenaRDF4J;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.app.OpenSaberApplication;
import io.opensaber.registry.authorization.AuthorizationToken;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.dao.impl.RegistryDaoImpl;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.service.impl.EncryptionServiceImpl;
import io.opensaber.registry.service.impl.RegistryServiceImpl;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.tests.utility.TestHelper;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;

import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.assertj.core.util.Arrays;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryController;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.MultipleEntityException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.registry.model.AuditRecord;
import io.opensaber.registry.schema.config.SchemaConfigurator;
import io.opensaber.registry.service.RegistryService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {OpenSaberApplication.class, RegistryController.class,
		GenericConfiguration.class, EncryptionServiceImpl.class, RegistryDaoImpl.class, AuditRecord.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryServiceImplTest extends RegistryTestBase {
	
	private static final String VALID_JSONLD = "school.jsonld";
	private static final String VALIDNEW_JSONLD = "school1.jsonld";
	private static final String CONTEXT_CONSTANT = "sample:";

	private boolean isInitialized = false;
	
	@Value("${registry.context.base}")
	private String registryContextBase;

	@Autowired
	private RegistryServiceImpl registryService;
	
	@Autowired
	private DatabaseProvider databaseProvider;

	@Mock
	private SchemaConfigurator mockSchemaConfigurator;

	@Mock
	private RestTemplate mockRestTemplate;

	@Mock
	private EncryptionServiceImpl encryptionService;

	@Mock
	private DatabaseProvider mockDatabaseProvider;

	@InjectMocks
	private RegistryServiceImpl registryServiceForHealth;
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	public void setup() {
		if (!isInitialized) {
			MockitoAnnotations.initMocks(this);
			ReflectionTestUtils.setField(encryptionService, "encryptionServiceHealthCheckUri", "encHealthCheckUri");
			ReflectionTestUtils.setField(encryptionService, "decryptionUri", "decryptionUri");
			ReflectionTestUtils.setField(encryptionService, "encryptionUri", "encryptionUri");
			isInitialized = true;
		}
	}

	@Before
	public void initialize() {
		setup();
		MockitoAnnotations.initMocks(this);
		TestHelper.clearData(databaseProvider);
		databaseProvider.getGraphStore().addVertex(Constants.GRAPH_GLOBAL_CONFIG).property(Constants.PERSISTENT_GRAPH, true);
        AuthInfo authInfo = new AuthInfo();
        authInfo.setAud("aud");
        authInfo.setName("name");
        authInfo.setSub("sub");
        AuthorizationToken authorizationToken = new AuthorizationToken(
                authInfo,
                Collections.singletonList(new SimpleGrantedAuthority("blah")));
        SecurityContextHolder.getContext().setAuthentication(authorizationToken);
	}

	@Test
	public void test_adding_a_new_record() throws DuplicateRecordException, EntityCreationException, EncryptionException, AuditFailedException, MultipleEntityException, RecordNotFoundException{
		Model model = getNewValidRdf(VALID_JSONLD, CONTEXT_CONSTANT);
		registryService.addEntity(model,null,null);
		assertEquals(5,
				IteratorUtils.count(databaseProvider.getGraphStore().traversal().clone().V()
						.filter(v -> !v.get().label().equalsIgnoreCase(Constants.GRAPH_GLOBAL_CONFIG))
						.hasNot(Constants.AUDIT_KEYWORD)));
	}
	
	@Test
	public void test_adding_duplicate_record() throws DuplicateRecordException, EntityCreationException, EncryptionException, AuditFailedException, MultipleEntityException, RecordNotFoundException {
		expectedEx.expect(DuplicateRecordException.class);
		expectedEx.expectMessage(Constants.DUPLICATE_RECORD_MESSAGE);
		Model model = getNewValidRdf(VALID_JSONLD, CONTEXT_CONSTANT);
		String entityId = registryService.addEntity(model,null,null);
		RDFUtil.updateRdfModelNodeId(model,
				ResourceFactory.createResource("http://example.com/voc/teacher/1.0.0/School"), entityId);
		registryService.addEntity(model,null,null);
	}
	
	@Test
	public void test_adding_record_with_no_entity() throws Exception {
		Model model = ModelFactory.createDefaultModel();
		expectedEx.expect(EntityCreationException.class);
		expectedEx.expectMessage(Constants.NO_ENTITY_AVAILABLE_MESSAGE);
		registryService.addEntity(model,null,null);
		closeDB();
	}
	
	@Test
	public void test_adding_record_with_more_than_one_entity() throws Exception {
		Model model = getNewValidRdf(VALID_JSONLD, CONTEXT_CONSTANT);
		model.add(getNewValidRdf(VALID_JSONLD, CONTEXT_CONSTANT));
		expectedEx.expect(MultipleEntityException.class);
		expectedEx.expectMessage(Constants.ADD_UPDATE_MULTIPLE_ENTITIES_MESSAGE);
		registryService.addEntity(model,null,null);
		closeDB();
	}

	@Test
	public void test_health_check_up_scenario() throws Exception {
		when(encryptionService.isEncryptionServiceUp()).thenReturn(true);
		when(mockDatabaseProvider.isDatabaseServiceUp()).thenReturn(true);
		HealthCheckResponse response = registryServiceForHealth.health();
		assertTrue(response.isHealthy());
		response.getChecks().forEach(ch -> assertTrue(ch.isHealthy()));
	}

	@Test
	public void test_health_check_down_scenario() throws Exception {
		when(encryptionService.isEncryptionServiceUp()).thenReturn(false);
		when(mockDatabaseProvider.isDatabaseServiceUp()).thenReturn(true);
		HealthCheckResponse response = registryServiceForHealth.health();
		assertFalse(response.isHealthy());
		response.getChecks().forEach(ch -> {
			if(ch.getName().equalsIgnoreCase(Constants.SUNBIRD_ENCRYPTION_SERVICE_NAME)) {
				assertFalse(ch.isHealthy());
			} else {
				assertTrue(ch.isHealthy());
			}
		});
	}
	
	@Test
	public void test_adding_record_with_multi_valued_literal_properties() throws Exception {
		Model model = getNewValidRdf(VALIDNEW_JSONLD);
		List<Resource> roots = RDFUtil.getRootLabels(model);
		String[] ct = {"I","II","III","IV"};
		List classesTaught = Arrays.asList(ct);
		for(Object obj : classesTaught){
			model.add(roots.get(0), ResourceFactory.createProperty(registryContextBase+"classesTaught"), (String)obj);
		}
		String response = registryService.addEntity(model,null,null);
		org.eclipse.rdf4j.model.Model responseModel = registryService.getEntityById(response);
		Model jenaModel = JenaRDF4J.asJenaModel(responseModel);
		assertTrue(jenaModel.isIsomorphicWith(model));
		closeDB();
	}

	public void closeDB() throws Exception{
		databaseProvider.shutdown();
	}
	
	

}
