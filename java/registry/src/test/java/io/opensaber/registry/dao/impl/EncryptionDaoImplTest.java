package io.opensaber.registry.dao.impl;

import io.opensaber.registry.authorization.AuthorizationToken;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.model.AuditRecordReader;
import io.opensaber.registry.schema.config.SchemaConfigurator;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.tests.utility.TestHelper;

import org.apache.jena.rdf.model.*;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.impl.EncryptionServiceImpl;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.InputStreamReader;
import java.util.*;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RegistryDaoImpl.class, Environment.class, ObjectMapper.class,
		GenericConfiguration.class, EncryptionServiceImpl.class, AuditRecordReader.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class EncryptionDaoImplTest extends RegistryTestBase {
	private static Logger logger = LoggerFactory.getLogger(EncryptionDaoImplTest.class);
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Autowired
	private RegistryDao registryDao;

	@Autowired
	private Gson gson;

	@Mock
	private EncryptionServiceImpl encryptionMock;

	@Mock
	private SchemaConfigurator mockSchemaConfigurator;

	@Autowired
	@InjectMocks
	private RegistryDaoImpl registryDaoImpl;

	private static Graph graph;

	@Autowired
	private DatabaseProvider databaseProvider;

	@Autowired
	AuditRecordReader auditRecordReader;

	@Value("${encryption.enabled}")
	private boolean encryptionEnabled;


	@Rule
	public TestRule watcher = new TestWatcher() {
		@Override
		protected void starting(Description description) {
			logger.debug("Executing test: " + description.getMethodName());
		}

		@Override
		protected void succeeded(Description description) {
			logger.debug("Successfully executed test: " + description.getMethodName());
		}

		@Override
		protected void failed(Throwable e, Description description) {
			logger.debug(String.format("Test %s failed. Error message: %s", description.getMethodName(), e.getMessage()));
		}
	};

	@Before
	public void initializeGraph() {
		Assume.assumeTrue(encryptionEnabled);
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

	public String updateGraphFromRdf(Model rdfModel) {

		return updateGraphFromRdf(rdfModel, graph);
	}

	private Map<String,Integer> getPropCounterMap(Graph entity) {
		Map<String,Integer> entityPropertyCountMap = new HashMap<>();
		Iterator<Vertex> iter = entity.traversal().clone().V().hasNot("@audit");
		while(iter.hasNext()){
			Vertex vertex = iter.next();
			entityPropertyCountMap.put(vertex.label(),0);
			Iterator<VertexProperty<Object>> propIter = vertex.properties();
			while(propIter.hasNext()){
				entityPropertyCountMap.put(vertex.label(),entityPropertyCountMap.get(vertex.label())+1);
				propIter.next();
			}
			Iterator<Edge> edgesIter = vertex.edges(Direction.OUT);
			while(edgesIter.hasNext()){
				entityPropertyCountMap.put(vertex.label(),entityPropertyCountMap.get(vertex.label())+1);
				edgesIter.next();
			}
		}
		return entityPropertyCountMap;
	}

	public void closeDB() throws Exception {
		databaseProvider.shutdown();
	}


	@After
	public void shutDown() throws Exception{
		if(graph!=null){
			graph.close();
		}
	}

	public Vertex getVertexForSubject(String subjectValue, String property, String objectValue){
		Vertex vertex = null;
		graph = TinkerGraph.open();
		GraphTraversalSource t = graph.traversal();
		GraphTraversal<Vertex, Vertex> hasLabel = t.V().hasLabel(subjectValue);
		if(hasLabel.hasNext()){
			vertex = hasLabel.next();
		} else {
			vertex = graph.addVertex(
					T.label,subjectValue);
		}
		vertex.property(property, objectValue);
		return vertex;
	}

	public Vertex getVertexWithMultipleProperties(String subjectValue, Map<String, Object> map){
		Vertex vertex = null;
		graph = TinkerGraph.open();
		GraphTraversalSource t = graph.traversal();
		GraphTraversal<Vertex, Vertex> hasLabel = t.V().hasLabel(subjectValue);
		if(hasLabel.hasNext()){
			vertex = hasLabel.next();
		} else {
			vertex = graph.addVertex(
					T.label,subjectValue);
		}
		for (Map.Entry<String, Object> entry : map.entrySet())
		{
			vertex.property(entry.getKey(), entry.getValue());
		}
		return vertex;
	}


	@Test
	public void test_encryption_for_nonEncryptableProperty() throws Exception {
		String label = generateRandomId();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("http://example.com/voc/teacher/1.0.0/schoolName", "ABC International School");
		map.put("http://example.com/voc/teacher/1.0.0/clusterResourceCentre", "test Cluster Resource");
		map.put("http://example.com/voc/teacher/1.0.0/udiseNumber", "1234");

		Map<String, Object> newMap = new HashMap<String, Object>();
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedclusterResourceCentre", "mockEncryptedClusterResourceCentre");
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedudiseNumber", "mockEncryptedUdiseNumber");

		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/schoolName")).thenReturn(false);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"))
		.thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/udiseNumber")).thenReturn(true);

		when(encryptionMock.encrypt(map)).thenReturn(newMap);
		//when(encryptionMock.encrypt("1234")).thenReturn("mockEncryptedUdiseNumber");

		getVertexWithMultipleProperties(label, map);
		String response = registryDao.addEntity(graph,label, null, null);
		registryDao.getEntityById(response);
		verify(encryptionMock, times(1)).encrypt(Mockito.anyMap());
	}

	@Test
	public void test_encryptionCall_for_encryptable_but_null_property() throws Exception {
		String label = generateRandomId();
		Map<String, Object> map = new HashMap<>();
		map.put("http://example.com/voc/teacher/1.0.0/schoolName", "ABC International School");
		map.put("http://example.com/voc/teacher/1.0.0/clusterResourceCentre", "test Cluster Resource");
		map.put("http://example.com/voc/teacher/1.0.0/udiseNumber", "1234");

		Map<String, Object> newMap = new HashMap<String, Object>();
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedclusterResourceCentre", "mockEncryptedClusterResourceCentre");
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedudiseNumber", "mockEncryptedUdiseNumber");

		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/schoolName")).thenReturn(false);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/clusterResourceCentre")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/udiseNumber")).thenReturn(true);

		when(encryptionMock.encrypt(map)).thenReturn(newMap);
		//when(encryptionMock.encrypt("1234")).thenReturn("mockEncryptedUdiseNumber");

		getVertexWithMultipleProperties(label, map);
		String response = registryDao.addEntity(graph, label, null, null);
		registryDao.getEntityById(response);

		verify(encryptionMock, times(1)).encrypt(Mockito.anyMap());
	}

	@Test
	public void test_encryptionCall_for_encryptable_multi_valued_property() throws Exception {
		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(new InputStreamReader
				(this.getClass().getClassLoader().getResourceAsStream("school.jsonld"))).getAsJsonObject();
		jsonObject.add("classesTaught", p.parse("[\"I\",\"II\",\"III\"]"));
		String dataString = gson.toJson(jsonObject);
		Model rdfModel = getNewValidRdfFromJsonString(dataString);
		String rootLabel = updateGraphFromRdf(rdfModel);

		Map<String, Object> map = new HashMap<>();
		map.put("http://example.com/voc/teacher/1.0.0/schoolName", "ABC International School");
		map.put("http://example.com/voc/teacher/1.0.0/clusterResourceCentre", "test Cluster Resource");
		map.put("http://example.com/voc/teacher/1.0.0/udiseNumber", "1234");

		Map<String, Object> newMap = new HashMap<String, Object>();
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedclusterResourceCentre", "mockEncryptedClusterResourceCentre");
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedudiseNumber", "mockEncryptedUdiseNumber");
		List encryptedClassList = new ArrayList();
		encryptedClassList.add("mockEncryptedClassesTaughtI");
		encryptedClassList.add("mockEncryptedClassesTaughtII");
		encryptedClassList.add("mockEncryptedClassesTaughtIII");
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedclassesTaught", encryptedClassList);

		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/schoolName")).thenReturn(false);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/clusterResourceCentre")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/udiseNumber")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/classesTaught")).thenReturn(true);

		when(encryptionMock.encrypt(map)).thenReturn(newMap);
		when(encryptionMock.encrypt("I")).thenReturn("mockEncryptedClassesTaughtI");
		when(encryptionMock.encrypt("II")).thenReturn("mockEncryptedClassesTaughtII");
		when(encryptionMock.encrypt("III")).thenReturn("mockEncryptedClassesTaughtIII");

		String response =registryDao.addEntity(graph, "_:"+rootLabel, null, null);
		registryDao.getEntityById(response);

		verify(encryptionMock, times(1)).encrypt(Mockito.anyMap());
		verify(encryptionMock, times(3)).encrypt(Mockito.anyString());
	}

	@Test
	public void test_encryptionServiceCall_for_null_property() throws Exception {
		Model rdfModel = getNewValidRdf();
		updateGraphFromRdf(rdfModel);
		GraphTraversal<Vertex, Vertex> gtvs = graph.traversal().clone().V();

		if (gtvs.hasNext()) {
			Vertex v = gtvs.next();
			Iterator<VertexProperty<Object>> iter = v.properties();
			while (iter.hasNext()) {
				VertexProperty<Object> property = iter.next();
				if (mockSchemaConfigurator.isPrivate(property.key()) && property.value() == null) {
					encryptionMock.encrypt(property.value());					
				}
			}
		}		
		verify(encryptionMock, never()).encrypt(Mockito.anyString());
	}

	@Test
	public void test_properties_single_node() throws Exception {
		String label = generateRandomId();
		Map<String,Object> map=new HashMap<>();
		map.put("http://example.com/voc/teacher/1.0.0/schoolName", "ABC International School");
		map.put("http://example.com/voc/teacher/1.0.0/clusterResourceCentre", "test Cluster Resource");
		map.put("http://example.com/voc/teacher/1.0.0/udiseNumber", "1234");

		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/schoolName")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/clusterResourceCentre")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/udiseNumber")).thenReturn(true);

		Map<String, Object> newMap = new HashMap<String, Object>();
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedclusterResourceCentre", "mockEncryptedClusterResourceCentre");
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedudiseNumber", "mockEncryptedUdiseNumber");
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedschoolName", "mockEncryptedSchoolName");

		when(encryptionMock.encrypt(map)).thenReturn(newMap);
		/* when(encryptionMock.encrypt("test Cluster Resource")).thenReturn("mockEncryptedClusterResourceCentre");
	    when(encryptionMock.encrypt("1234")).thenReturn("mockEncryptedUdiseNumber");
		 */
		when(mockSchemaConfigurator.isEncrypted("encryptedschoolName")).thenReturn(true);
		when(mockSchemaConfigurator.isEncrypted("encryptedclusterResourceCentre")).thenReturn(true);
		when(mockSchemaConfigurator.isEncrypted("encryptedudiseNumber")).thenReturn(true);

		when(encryptionMock.decrypt(newMap)).thenReturn(map);
		/*when(encryptionMock.decrypt("mockEncryptedClusterResourceCentre")).thenReturn("test Cluster Resource");
	    when(encryptionMock.decrypt("mockEncryptedUdiseNumber")).thenReturn("1234");*/

		getVertexWithMultipleProperties(label,map);		
		String response = registryDao.addEntity(graph, label, null, null);
		registryDao.getEntityById(response);		

		verify(encryptionMock, times(1)).encrypt(Mockito.anyMap());
		verify(encryptionMock, times(1)).decrypt(Mockito.anyMap());
	}

	@Test
	public void test_properties_multi_node() throws Exception {
		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(new InputStreamReader
				(this.getClass().getClassLoader().getResourceAsStream("school.jsonld"))).getAsJsonObject();
		JsonObject addressObject = jsonObject.getAsJsonObject("sample:address");
		addressObject.addProperty("street", "1st main");
		String dataString = gson.toJson(jsonObject);
		System.out.println("dataString=="+dataString);
		Model rdfModel = getNewValidRdfFromJsonString(dataString);
		String rootLabel = updateGraphFromRdf(rdfModel);

		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/schoolName")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/clusterResourceCentre")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/udiseNumber")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/street")).thenReturn(true);

		Map<String,Object> map=new HashMap<>();
		map.put("http://example.com/voc/teacher/1.0.0/schoolName", "Bluebells");
		map.put("http://example.com/voc/teacher/1.0.0/clusterResourceCentre", "some Cluster Resource");
		map.put("http://example.com/voc/teacher/1.0.0/udiseNumber", "9876");
		map.put("http://example.com/voc/teacher/1.0.0/street", "1st main");


		Map<String, Object> newMap = new HashMap<String, Object>();
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedclusterResourceCentre", "mockEncryptedClusterResourceCentre");
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedudiseNumber", "mockEncryptedUdiseNumber");
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedschoolName", "mockEncryptedSchoolName");
		newMap.put("http://example.com/voc/teacher/1.0.0/encryptedStreet", "mockEncryptedStreet");

		when(encryptionMock.encrypt(map)).thenReturn(newMap);


		when(mockSchemaConfigurator.isEncrypted("encryptedschoolName")).thenReturn(true);
		when(mockSchemaConfigurator.isEncrypted("encryptedclusterResourceCentre")).thenReturn(true);
		when(mockSchemaConfigurator.isEncrypted("encryptedudiseNumber")).thenReturn(true);
		when(mockSchemaConfigurator.isEncrypted("encryptedStreet")).thenReturn(true);

		when(encryptionMock.decrypt(newMap)).thenReturn(map);


		String response =registryDao.addEntity(graph, "_:"+rootLabel, null, null);					
		registryDao.getEntityById(response);

		verify(encryptionMock, times(1)).encrypt(Mockito.anyMap());	
		verify(encryptionMock, times(1)).decrypt(Mockito.anyMap());		
	}

	@Test
	public void test_encryption_EncryptionError() throws Exception {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		when(encryptionMock.encrypt(Mockito.anyString())).thenThrow(EncryptionException.class);

		try {
			registryDao.addEntity(graph, rootLabel, null, null);
		} catch (EncryptionException e) {
			assertThat(e.toString(), allOf(containsString("EncryptionException")));
		}
	}

	@Test
	public void test_decryption_EncryptionException() throws Exception {
		Model rdfModel = getNewValidRdf();
		String label = updateGraphFromRdf(rdfModel);
		when(encryptionMock.decrypt(Mockito.anyString())).thenThrow(EncryptionException.class);

		try {
			String response = registryDao.addEntity(graph, String.format("_:%s", label), null, null);
			registryDao.getEntityById(response);
		} catch (EncryptionException e) {
			assertThat(e.toString(), allOf(containsString("EncryptionException")));
		}
	}



}
