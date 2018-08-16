package io.opensaber.registry.service.impl;

import static org.junit.Assert.*;
import java.util.Collections;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import io.opensaber.registry.app.OpenSaberApplication;
import io.opensaber.registry.authorization.AuthorizationToken;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.dao.SearchDao;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.MultipleEntityException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.exception.TypeNotProvidedException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.service.SearchService;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.tests.utility.TestHelper;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {OpenSaberApplication.class, RegistryDao.class,
		SearchDao.class, SearchService.class, GenericConfiguration.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class SearchServiceImplTest extends RegistryTestBase{
	
	private static final String BASE_SEARCH_JSONLD = "base_search_context.jsonld";
	private static final String CONTEXT_NAMESPACE =  "http://example.com/voc/teacher/1.0.0/";
	
	@Autowired
	private SearchService searchService;
	
	@Autowired
	private RegistryService registryService;
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	@Autowired
	private DatabaseProvider databaseProvider;
	
	@Before
	public void initialize() {
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
	public void test_search_no_response() throws AuditFailedException, EncryptionException, RecordNotFoundException, TypeNotProvidedException {
		Model rdf = getNewValidRdf(BASE_SEARCH_JSONLD);
		List<Resource> subjectList = RDFUtil.getRootLabels(rdf);
		Property property = ResourceFactory.createProperty(CONTEXT_NAMESPACE + "schoolName");
		rdf.add(subjectList.get(0), property, "Bluebells");
		org.eclipse.rdf4j.model.Model responseModel = searchService.search(rdf);
		assertTrue(responseModel.isEmpty());
	}
	
	@Test
	public void test_search_valid_response() throws AuditFailedException, EncryptionException, RecordNotFoundException, 
	TypeNotProvidedException, EntityCreationException, MultipleEntityException, DuplicateRecordException {
		String response = addEntity();
		Model rdf = getNewValidRdf(BASE_SEARCH_JSONLD);
		List<Resource> subjectList = RDFUtil.getRootLabels(rdf);
		Property property = ResourceFactory.createProperty(CONTEXT_NAMESPACE + "schoolName");
		rdf.add(subjectList.get(0), property, "Bluebells");
		org.eclipse.rdf4j.model.Model responseModel = searchService.search(rdf);
		assertFalse(responseModel.isEmpty());
		ValueFactory vf = SimpleValueFactory.getInstance();
		assertTrue(responseModel.contains(vf.createIRI(response), (IRI)null, (org.eclipse.rdf4j.model.Resource)null));
	}
	
	@Test
	public void test_search_with_no_type_provided() throws AuditFailedException, EncryptionException, RecordNotFoundException, TypeNotProvidedException {
		expectedEx.expect(TypeNotProvidedException.class);
		expectedEx.expectMessage(Constants.ENTITY_TYPE_NOT_PROVIDED);
		Model rdf = getNewValidRdf(BASE_SEARCH_JSONLD);
		List<Resource> subjectList = RDFUtil.getRootLabels(rdf);
		Property property = ResourceFactory.createProperty(CONTEXT_NAMESPACE + "schoolName");
		rdf.add(subjectList.get(0), property, "Bluebells");
		rdf.removeAll(null, RDF.type, ResourceFactory.createResource(CONTEXT_NAMESPACE+"School"));
		searchService.search(rdf);
	}
	
	private String addEntity() throws DuplicateRecordException, AuditFailedException, 
	EncryptionException, RecordNotFoundException, MultipleEntityException, EntityCreationException{
		Model rdfModel = getNewValidRdf();
		return registryService.addEntity(rdfModel, null, null);
	}

}
