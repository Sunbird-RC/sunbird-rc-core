package io.opensaber.registry.dao.impl;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import io.opensaber.pojos.Filter;
import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.app.OpenSaberApplication;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.SearchDao;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { OpenSaberApplication.class, IRegistryDao.class, SearchDao.class,
		GenericConfiguration.class })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class SearchDaoImplTest extends RegistryTestBase {

	private static Graph graph;
	@Autowired
	private IRegistryDao registryDao;
	@Autowired
	private SearchDao searchDao;

	@Before
	public void initializeGraph() throws IOException {
	    graph = TinkerGraph.open();
	}

	@Test
	public void test_search_no_response() throws AuditFailedException, EncryptionException, RecordNotFoundException {
		SearchQuery searchQuery = new SearchQuery("");
		JsonNode result = searchDao.search(graph, searchQuery);
		assertTrue(result.get("").asText().isEmpty());
	}


	private SearchQuery getSearchQuery(SearchQuery searchQuery, Filter filter, String type) {
		List<Filter> filterList = new ArrayList<Filter>();
		if (searchQuery.getFilters() != null) {
			filterList = searchQuery.getFilters();
		}
		filterList.add(filter);
		searchQuery.setFilters(filterList);
		searchQuery.setRootLabel(type);
		return searchQuery;
	}

	private Filter getFilterEqual(String property, String value) {
		Filter filter = new Filter(property, "=", value);
		return filter;
	}
}
