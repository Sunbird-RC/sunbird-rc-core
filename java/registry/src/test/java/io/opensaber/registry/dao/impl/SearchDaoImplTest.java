package io.opensaber.registry.dao.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import io.opensaber.converters.JenaRDF4J;
import io.opensaber.pojos.Filter;
import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.app.OpenSaberApplication;
import io.opensaber.registry.authorization.AuthorizationToken;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.tests.utility.TestHelper;
import io.opensaber.utils.converters.RDF2Graph;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.dao.SearchDao;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.MultipleEntityException;
import io.opensaber.registry.exception.RecordNotFoundException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {OpenSaberApplication.class,RegistryDao.class, SearchDao.class,
		GenericConfiguration.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class SearchDaoImplTest extends RegistryTestBase {
	
	@Autowired
	private RegistryDao registryDao;
	
	@Autowired
	private SearchDao searchDao;
	
	private static Graph graph;
	
	@Autowired
	private DatabaseProvider databaseProvider;
	
	@Before
	public void initializeGraph() {
		graph = TinkerGraph.open();
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
	public void test_search_no_response() throws AuditFailedException, EncryptionException, RecordNotFoundException {
		SearchQuery searchQuery = new SearchQuery();
		Map<String, Graph> responseGraph = searchDao.search(searchQuery);
		assertTrue(responseGraph.isEmpty());
	}
	
	@Test
	public void test_search_by_property() throws AuditFailedException, EncryptionException, RecordNotFoundException, DuplicateRecordException {
		String response = addEntity();
		SearchQuery searchQuery = new SearchQuery();
		searchQuery = getSearchQuery(searchQuery,getFilter("http://example.com/voc/teacher/1.0.0/schoolName", "Bluebells"),"http://example.com/voc/teacher/1.0.0/School");
		Map<String, Graph> responseGraph = searchDao.search(searchQuery);
		assertTrue(!responseGraph.isEmpty());
		assertTrue(responseGraph.size() == 1);
		assertTrue(responseGraph.containsKey(response));
		
	}
	
	@Test
	public void test_search_by_edge() throws AuditFailedException, EncryptionException, RecordNotFoundException, DuplicateRecordException {
		String response = addEntity();
		SearchQuery searchQuery = new SearchQuery();
		searchQuery = getSearchQuery(searchQuery,getFilter("http://example.com/voc/teacher/1.0.0/area","http://example.com/voc/teacher/1.0.0/AreaTypeCode-URBAN"),"http://example.com/voc/teacher/1.0.0/School");
		Map<String, Graph> responseGraph = searchDao.search(searchQuery);
		assertTrue(!responseGraph.isEmpty());
		assertTrue(responseGraph.size() == 1);
		assertTrue(responseGraph.containsKey(response));
	}
	
	
	@Test
	public void test_search_multiple_records_by_property() throws AuditFailedException, EncryptionException, RecordNotFoundException, DuplicateRecordException {
		String response1 = addEntity();
		String response2 = addEntity();
		String response3 = addEntity();
		SearchQuery searchQuery = new SearchQuery();
		searchQuery = getSearchQuery(searchQuery,getFilter("http://example.com/voc/teacher/1.0.0/schoolName", "Bluebells"),"http://example.com/voc/teacher/1.0.0/School");
		Map<String, Graph> responseGraph = searchDao.search(searchQuery);
		assertTrue(!responseGraph.isEmpty());
		assertTrue(responseGraph.size() == 3);
		assertTrue(responseGraph.containsKey(response1));
		assertTrue(responseGraph.containsKey(response2));
		assertTrue(responseGraph.containsKey(response3));
	}
	

	@Test
	public void test_search_multiple_records_by_edge() throws AuditFailedException, EncryptionException, RecordNotFoundException, DuplicateRecordException {
		String response1 = addEntity();
		String response2 = addEntity();
		String response3 = addEntity();
		SearchQuery searchQuery = new SearchQuery();
		searchQuery = getSearchQuery(searchQuery,getFilter("http://example.com/voc/teacher/1.0.0/area","http://example.com/voc/teacher/1.0.0/AreaTypeCode-URBAN"),"http://example.com/voc/teacher/1.0.0/School");
		Map<String, Graph> responseGraph = searchDao.search(searchQuery);
		assertTrue(!responseGraph.isEmpty());
		assertTrue(responseGraph.size() == 3);
		assertTrue(responseGraph.containsKey(response1));
		assertTrue(responseGraph.containsKey(response2));
		assertTrue(responseGraph.containsKey(response3));
	}
	
	@Test
	public void test_search_before_and_after_updating_property_and_adge() throws AuditFailedException, MultipleEntityException, 
	EncryptionException, RecordNotFoundException, DuplicateRecordException, EntityCreationException {
		String response = addEntity();
		SearchQuery searchQuery = new SearchQuery();
		searchQuery = getSearchQuery(searchQuery,getFilter("http://example.com/voc/teacher/1.0.0/schoolName", "Bluebells"),"http://example.com/voc/teacher/1.0.0/School");
		searchQuery = getSearchQuery(searchQuery,getFilter("http://example.com/voc/teacher/1.0.0/area","http://example.com/voc/teacher/1.0.0/AreaTypeCode-URBAN"),"http://example.com/voc/teacher/1.0.0/School");
		Map<String, Graph> responseGraph = searchDao.search(searchQuery);
		assertTrue(!responseGraph.isEmpty());
		assertTrue(responseGraph.size() == 1);
		assertTrue(responseGraph.containsKey(response));
		Graph entity = registryDao.getEntityById(response);
		Model addedModel = JenaRDF4J.asJenaModel(RDF2Graph.convertGraph2RDFModel(entity, response));
		removeStatementFromModel(addedModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/schoolName"));
		addedModel.add(ResourceFactory.createResource(response), 
				ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/schoolName"), "BVM");
		Graph updateGraph = TinkerGraph.open();
		updateGraph = generateGraphFromRDF(updateGraph, addedModel);
		registryDao.updateEntity(updateGraph, response, "update");
		responseGraph = searchDao.search(searchQuery);
		assertTrue(responseGraph.isEmpty());
		assertTrue(responseGraph.size() == 0);
	}
	
	private String addEntity() throws DuplicateRecordException, AuditFailedException, EncryptionException, RecordNotFoundException{
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		return registryDao.addEntity(graph, "_:"+rootLabel, null, null);
	}
	
	private SearchQuery getSearchQuery(SearchQuery searchQuery, Filter filter, String type){
		List<Filter> filterList = new ArrayList<Filter>();
		if(searchQuery.getFilters() != null){
			filterList = searchQuery.getFilters();
		}
		filterList.add(filter);
		searchQuery.setFilters(filterList);
		searchQuery.setType(type);
		searchQuery.setTypeIRI(RDF.type.toString());
		return searchQuery;
	}

	private Filter getFilter(String property, String value){
		Filter filter = new Filter();
		filter.setProperty(property);
		filter.setValue(value);
		return filter;
	}
}
