package io.opensaber.registry.controller;

import static org.junit.Assert.*;

import io.opensaber.registry.app.OpenSaberApplication;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.tests.utility.TestHelper;
import io.opensaber.registry.util.RDFUtil;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.InvalidTypeException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RegistryService;

import org.apache.jena.rdf.model.Model;

@RunWith(SpringRunner.class)
@SpringBootTest(classes={OpenSaberApplication.class, RegistryController.class, GenericConfiguration.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryControllerTest extends RegistryTestBase{
	
	private static final String VALID_JSONLD = "school.jsonld";
	private static final String CONTEXT_CONSTANT = "sample:";

	@Autowired
	private RegistryService registryService;
	
	@Autowired
	private DatabaseProvider databaseProvider;

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Before
	public void initialize() {
		TestHelper.clearData(databaseProvider);
	}

	@Test
	public void test_adding_a_new_record() throws DuplicateRecordException, InvalidTypeException, EncryptionException, AuditFailedException {
		Model model = getNewValidRdf(VALID_JSONLD, CONTEXT_CONSTANT);
		String entityId = registryService.addEntity(model);
		assertEquals(5, IteratorUtils.count(databaseProvider.getGraphStore().traversal().clone().V().hasNot("@audit")));
	}
	
	@Test
	public void test_adding_duplicate_record() throws DuplicateRecordException, InvalidTypeException, EncryptionException, AuditFailedException {
		expectedEx.expect(DuplicateRecordException.class);
		expectedEx.expectMessage(Constants.DUPLICATE_RECORD_MESSAGE);
		Model model = getNewValidRdf(VALID_JSONLD, CONTEXT_CONSTANT);
		String entityId = registryService.addEntity(model);
		RDFUtil.updateRdfModelNodeId(model, ResourceFactory.createResource("http://example.com/voc/teacher/1.0.0/School"), entityId);
		registryService.addEntity(model);
	}
	
	@Test
	public void test_adding_record_with_invalid_type() throws DuplicateRecordException, InvalidTypeException, Exception {
		Model model = getRdfWithInvalidTpe();
		expectedEx.expect(InvalidTypeException.class);
		expectedEx.expectMessage(Constants.INVALID_TYPE_MESSAGE);
		registryService.addEntity(model);
		closeDB();
	}

	public void closeDB() throws Exception{
		databaseProvider.shutdown();
	}

}
