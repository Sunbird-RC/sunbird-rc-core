package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sunbirdrc.registry.dao.VertexWriter;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.DBConnectionInfo;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.service.NativeSearchService;
import dev.sunbirdrc.registry.sink.DBProviderFactory;
import dev.sunbirdrc.registry.sink.DatabaseProvider;
import dev.sunbirdrc.registry.sink.shard.DefaultShardAdvisor;
import dev.sunbirdrc.registry.sink.shard.ShardManager;
import dev.sunbirdrc.registry.util.Definition;
import dev.sunbirdrc.registry.util.DefinitionsManager;
import dev.sunbirdrc.registry.util.OSResourceLoader;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.keycloak.common.util.RandomString;
import org.kie.api.runtime.manager.audit.AuditService;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collections;

import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_LIST;
import static dev.sunbirdrc.registry.middleware.util.Constants.TOTAL_COUNT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DefinitionsManager.class, ObjectMapper.class, DBProviderFactory.class, DBConnectionInfoMgr.class,
		OSResourceLoader.class, ShardManager.class, DefaultShardAdvisor.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class NativeSearchServiceTest {

	private static Graph graph;
	private DatabaseProvider databaseProvider;
	@Autowired
	private DefinitionsManager definitionsManager;
	@Autowired
	private DBProviderFactory dbProviderFactory;
	@Autowired
	private DBConnectionInfoMgr dbConnectionInfoMgr;
	@Autowired
	private ShardManager shardManager;
	@Mock
	private AuditService auditService;

	private NativeSearchService nativeSearchService;


	@Before
	public void init() throws IOException {
		dbConnectionInfoMgr.setUuidPropertyName("tid");
		DBConnectionInfo dbConnectionInfo = new DBConnectionInfo();
		dbConnectionInfo.setShardId(RandomString.randomCode(4));
		dbConnectionInfo.setShardLabel("");
		dbConnectionInfoMgr.setConnectionInfo(Collections.singletonList(dbConnectionInfo));

		databaseProvider = dbProviderFactory.getInstance(dbConnectionInfo);
		graph = databaseProvider.getOSGraph().getGraphStore();
		populateGraph();
		createTeacherDefinition();
		nativeSearchService = new NativeSearchService();
		ReflectionTestUtils.setField(nativeSearchService, "definitionsManager", definitionsManager);
		ReflectionTestUtils.setField(nativeSearchService, "dbConnectionInfoMgr", dbConnectionInfoMgr);
		ReflectionTestUtils.setField(nativeSearchService, "shardManager", shardManager);
		ReflectionTestUtils.setField(nativeSearchService, "limit", 100);
		ReflectionTestUtils.setField(nativeSearchService, "uuidPropertyName", "osid");

	}


	@Test
	public void shouldReturnRecordsMatchingFilters() throws IOException {
		JsonNode query = getSearchQuery();
		JsonNode results = nativeSearchService.search(query, "");
		Assert.assertEquals(1L, results.get("Teacher").get(TOTAL_COUNT).asLong());
	}

	private JsonNode getSearchQuery() throws JsonProcessingException {
		JsonNode query = new ObjectMapper().readTree("{\n" +
				"  \"entityType\": [\"Teacher\"],\n" +
				"  \"filters\": {\n" +
				"    \"teacherName\": {\n" +
				"      \"eq\": \"ram\"\n" +
				"    }\n" +
				"  }\n" +
				"}");
		return query;
	}

	@Test
	public void shouldRemovePublicFields() throws IOException {
		ReflectionTestUtils.setField(nativeSearchService, "removeNonPublicFieldsForNativeSearch", true);
		JsonNode query = getSearchQuery();
		JsonNode results = nativeSearchService.search(query, "");
		Assert.assertEquals(1L, results.get("Teacher").get(TOTAL_COUNT).asLong());
		Assert.assertEquals(4, results.get("Teacher").get(ENTITY_LIST).get(0).size());
		Assert.assertNull(results.get("Teacher").get(ENTITY_LIST).get(0).get("serialNum"));

	}

	@Test
	public void shouldNotRemovePublicFields() throws IOException {
		ReflectionTestUtils.setField(nativeSearchService, "removeNonPublicFieldsForNativeSearch", false);
		JsonNode query = getSearchQuery();
		JsonNode results = nativeSearchService.search(query, "");
		Assert.assertEquals(1L, results.get("Teacher").get(TOTAL_COUNT).asLong());
		Assert.assertEquals(5, results.get("Teacher").get(ENTITY_LIST).get(0).size());
		Assert.assertNotNull(results.get("Teacher").get(ENTITY_LIST).get(0).get("serialNum"));
	}


	@PreDestroy
	public void shutdown() throws Exception {

		graph.close();
	}


	private void populateGraph() {
		VertexWriter vertexWriter = new VertexWriter(graph, databaseProvider, "tid");
		Vertex v1 = vertexWriter.createVertex("Teacher");
		v1.property("serialNum", 1);
		v1.property("teacherName", "mark");
		Vertex v2 = vertexWriter.createVertex("Teacher");
		v2.property("serialNum", 2);
		v2.property("teacherName", "zuer");
		Vertex v3 = vertexWriter.createVertex("Teacher");
		v3.property("serialNum", 3);
		v3.property("teacherName", "ram");
	}

	private void createTeacherDefinition() throws JsonProcessingException {
		Definition definition = new Definition(new ObjectMapper().readTree("{\n" +
				"  \"$schema\": \"http://json-schema.org/draft-07/schema\",\n" +
				"  \"type\": \"object\",\n" +
				"  \"properties\": {\n" +
				"    \"Teacher\": {\n" +
				"      \"$ref\": \"#/definitions/Teacher\"\n" +
				"    }\n" +
				"  },\n" +
				"  \"required\": [\n" +
				"    \"Teacher\"\n" +
				"  ],\n" +
				"  \"title\":\"Teacher\",\n" +
				"  \"definitions\": {\n" +
				"    \"Teacher\": {\n" +
				"      \"$id\": \"#/properties/Teacher\",\n" +
				"      \"type\": \"object\",\n" +
				"      \"title\": \"The Teacher Schema\",\n" +
				"      \"required\": [\n" +
				"\n" +
				"      ],\n" +
				"      \"properties\": {\n" +
				"        \"serialNum\": {\n" +
				"          \"type\": \"string\"\n" +
				"        },\n" +
				"        \"teacherName\": {\n" +
				"          \"type\": \"string\"\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  },\n" +
				"  \"_osConfig\": {\n" +
				"    \"internalFields\": [\"$.serialNum\"]\n" +
				"  }\n" +
				"}\n"));
		definitionsManager.appendNewDefinition(definition);
	}

}
