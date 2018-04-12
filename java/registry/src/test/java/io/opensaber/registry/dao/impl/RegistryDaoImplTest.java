package io.opensaber.registry.dao.impl;

import io.opensaber.registry.authorization.AuthorizationToken;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.audit.LabelCannotBeNullException;
import io.opensaber.registry.model.AuditRecordReader;
import io.opensaber.registry.schema.config.SchemaConfigurator;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.tests.utility.TestHelper;
import io.opensaber.registry.util.RDFUtil;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.Property;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import io.opensaber.converters.JenaRDF4J;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.EncryptionService;
import io.opensaber.registry.service.impl.EncryptionServiceImpl;
import io.opensaber.utils.converters.RDF2Graph;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.*;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RegistryDaoImpl.class, Environment.class, ObjectMapper.class, GenericConfiguration.class,EncryptionServiceImpl.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryDaoImplTest extends RegistryTestBase {
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	@Autowired
	private Environment environment;
	
	@Autowired
	private RegistryDao registryDao;
	
	@Mock
	private EncryptionService encryptionMock;
	
	@Mock
	private SchemaConfigurator mockSchemaConfigurator;
	
	@Autowired
	@InjectMocks
	RegistryDaoImpl registryDaoImpl;
	
  	private static Graph graph;
			
	@Autowired
	private DatabaseProvider databaseProvider;
	
	@Value("${registry.system.base}")
	private String registrySystemContext="http://example.com/voc/opensaber/";

	private static String identifier;

	private static final String VALID_JSONLD = "school.jsonld";
    private static final String RICH_LITERAL_TTL = "rich-literal.jsonld";
	private static final String CONTEXT_CONSTANT = "sample:";
	private static final String CONFIG_SCHEMA_FILE = "opensaber-schema-configuration-school-test.jsonld";
	
	private SchemaConfigurator schemaConfigurator;
	private void initialize(String file) throws IOException{
		schemaConfigurator = new SchemaConfigurator(file);
	}
	
	@Rule
	public TestRule watcher = new TestWatcher() {
		@Override
		protected void starting(Description description) {
			System.out.println("Executing test: " + description.getMethodName());
		}

		@Override
		protected void succeeded(Description description) {
			System.out.println("Successfully executed test: " + description.getMethodName());
		}

		@Override
		protected void failed(Throwable e, Description description) {
			System.out.println(String.format("Test %s failed. Error message: %s", description.getMethodName(), e.getMessage()));
		}
	};

	@Before
	public void initializeGraph() {
		graph = TinkerGraph.open();
		MockitoAnnotations.initMocks(this);
		TestHelper.clearData(databaseProvider);
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
	public void test_adding_a_single_node() throws DuplicateRecordException, RecordNotFoundException, EncryptionException, LabelCannotBeNullException, AuditFailedException {
		String label = generateRandomId();
		identifier = label;
		getVertexForSubject(label, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		String response = registryDao.addEntity(graph, label);
		Graph entity = registryDao.getEntityById(response);
		assertEquals(1, IteratorUtils.count(entity.traversal().clone().V().hasNot(registrySystemContext+"audit")));
		Vertex v = entity.traversal().V().has(T.label, label).next();
		assertEquals("DAV Public School", v.property("http://example.com/voc/teacher/1.0.0/schoolName").value());
        checkIfAuditRecordsAreRight(entity, null);
	}

    private int checkIfAuditRecordsAreRight(Graph entity, Map<String, Map<String, Integer>> updateCountMap) throws LabelCannotBeNullException {
	    System.out.println("ADJUSTMENT MAP="+updateCountMap);
	    int count=0;
	    int adjustedCount;
        Iterator it = getPropCounterMap(entity).entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String,Integer> pair = (Map.Entry)it.next();
            int updatedPropertyCount=0;
            System.out.println("updateCountMap "+updateCountMap+" "+pair.getKey());
            if(updateCountMap!=null) {
                Map<String, Integer> labelPropertyMap = updateCountMap.get(String.valueOf(pair.getKey()));
                if (labelPropertyMap != null) {
                    updatedPropertyCount = labelPropertyMap.values().stream().mapToInt(i->i).sum();;
                }
            }
            adjustedCount = pair.getValue().intValue()+updatedPropertyCount;
            System.out.println(pair.getKey() + " = " + adjustedCount);
            count+=adjustedCount;
            assertEquals(adjustedCount, new AuditRecordReader(databaseProvider).fetchAuditRecords(String.valueOf(pair.getKey()),null).size());
            it.remove();
        }
        return count;
    }

    public String updateGraphFromRdf(Model rdfModel) {
		 
        return updateGraphFromRdf(rdfModel, graph, environment.getProperty(Constants.SUBJECT_LABEL_TYPE));
    }

    public String updateGraphFromRdf(Model rdfModel, Graph graph) {
	 return updateGraphFromRdf(rdfModel, graph, environment.getProperty(Constants.SUBJECT_LABEL_TYPE));
 }

    public String updateGraphFromRdf(Model rdfModel, Graph graph, String rootLabelType) {
	StmtIterator iterator = rdfModel.listStatements();
	boolean rootSubjectFound = false;
	String label = null;
	while (iterator.hasNext()) {
		Statement rdfStatement = iterator.nextStatement();
			if (!rootSubjectFound) {
				label = RDF2Graph.getRootSubjectLabel(rdfStatement, rootLabelType);
				if (label != null) {
					rootSubjectFound = true;
				}
			}
			org.eclipse.rdf4j.model.Statement rdf4jStatement = JenaRDF4J.asrdf4jStatement(rdfStatement);
			graph = RDF2Graph.convertRDFStatement2Graph(rdf4jStatement, graph);
		}
		return label;
}
    private Map<String,Integer> getPropCounterMap(Graph entity) {
	    Map<String,Integer> entityPropertyCountMap = new HashMap<>();
        Iterator<Vertex> iter = entity.traversal().clone().V().hasNot(registrySystemContext+"audit");
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

    @Test @Ignore
	public void test_adding_existing_root_node() throws NullPointerException, DuplicateRecordException, EncryptionException, AuditFailedException {
		getVertexForSubject(identifier, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		expectedEx.expect(DuplicateRecordException.class);
		expectedEx.expectMessage(Constants.DUPLICATE_RECORD_MESSAGE);
		registryDao.addEntity(graph, identifier);
	}
	
	@Test
	public void test_adding_multiple_nodes() throws NullPointerException, DuplicateRecordException, RecordNotFoundException, EncryptionException, AuditFailedException, LabelCannotBeNullException {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel));
		System.out.println(response);
		Graph entity = registryDao.getEntityById(response);
		long vertexCount = IteratorUtils.count(entity.traversal().clone().V());
		assertEquals(5, vertexCount);
        checkIfAuditRecordsAreRight(entity, null);
//		Iterator mapIter = getPropCounterMap(entity);
//        assertEquals(getPropCounterMap(entity), new AuditRecordReader(databaseProvider).fetchAuditRecords(response,null).size());
	}

    @Test
	public void test_adding_shared_nodes() throws LabelCannotBeNullException {
		try {
			Model rdfModel1 = getNewValidRdf();
			TinkerGraph graphEntity1 = TinkerGraph.open();
			String rootLabelEntity1 = createGraphFromRdf(graphEntity1, rdfModel1);
			String entity1Label = registryDao.addEntity(graphEntity1, "_:" + rootLabelEntity1);

			Graph entity1 = registryDao.getEntityById(entity1Label);
            checkIfAuditRecordsAreRight(entity1, null);

			// Expected count of vertices in one entity
			assertEquals(5, IteratorUtils.count(entity1.traversal().V()));

			Model rdfModel2 = getNewValidRdf();
			TinkerGraph graphEntity2 = TinkerGraph.open();
			String rootLabelEntity2 = createGraphFromRdf(graphEntity2, rdfModel2);
			String entity2Label = registryDao.addEntity(graphEntity2, "_:" + rootLabelEntity2);

			Graph entity2 = registryDao.getEntityById(entity2Label);
            checkIfAuditRecordsAreRight(entity2, null);

			assertEquals(5, IteratorUtils.count(entity2.traversal().V()));

			long verticesCountAfterSharedNodesCreation = IteratorUtils.count(databaseProvider.getGraphStore().traversal().clone().V().hasNot(registrySystemContext+"audit"));
			long edgesCountAfterSharedNodesCreation = IteratorUtils.count(databaseProvider.getGraphStore().traversal().clone().E().hasNot(registrySystemContext+"audit"));

			// Expected count of vertices is 6 with two entities with same address created
			assertEquals(7, verticesCountAfterSharedNodesCreation);
			assertEquals(8, edgesCountAfterSharedNodesCreation);

		} catch (DuplicateRecordException | RecordNotFoundException | EncryptionException | NoSuchElementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuditFailedException e) {
            e.printStackTrace();
        }
    }
	
	@Test
	public void test_adding_shared_nodes_with_new_properties() throws DuplicateRecordException, RecordNotFoundException, EncryptionException, AuditFailedException, LabelCannotBeNullException {

		// Add a new entity
		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		System.out.println("PRINTING MODEL TO ADD");
        printModel(rdfModel);
        String newEntityResponse;
        newEntityResponse = registryDao.addEntity(graph, String.format("_:%s", rootLabel));
        Graph entity = registryDao.getEntityById(newEntityResponse);
        System.out.println("CHECKING AUDIT RECORDS");
        int count1 = checkIfAuditRecordsAreRight(entity, null);
        System.out.println("AUDIT RECORDS "+count1);
		// Create a new TinkerGraph with the existing jsonld
		Graph newEntityGraph = TinkerGraph.open();
		Model newRdfModel = getNewValidRdf();
		updateNodeLabel(newRdfModel, "http://example.com/voc/teacher/1.0.0/IndianUrbanPostalAddress");

		String addressLabel = databaseProvider.getGraphStore().traversal().clone().V()
				.has(T.label, "http://example.com/voc/teacher/1.0.0/IndianUrbanPostalAddress")
				.next().vertices(Direction.IN).next().label();

		// Add a new property to the existing address node
		Resource resource = ResourceFactory.createResource(addressLabel);
		Property predicate = ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/districtAlias");
		Literal literal = ResourceFactory.createPlainLiteral("Gurgaon alias");
		newRdfModel.add(ResourceFactory.createStatement(resource, predicate, literal));
		String newRootLabel = updateGraphFromRdf(newRdfModel, newEntityGraph);
        printModel(newRdfModel);
		newEntityResponse = registryDao.addEntity(newEntityGraph, String.format("_:%s", newRootLabel));
		entity = registryDao.getEntityById(newEntityResponse);
        int count2 = checkIfAuditRecordsAreRight(entity, null);
        System.out.println("AUDIT RECORDS "+count2);
		String propertyValue = (String) entity.traversal().clone().V()
			.properties("http://example.com/voc/teacher/1.0.0/districtAlias")
			.next()
			.value();
		assertEquals("Gurgaon alias", propertyValue);
		assertEquals(1,count2-count1);

	}

    private void printModel(Model rdfModel) {
        Iterator iter = rdfModel.listStatements();
        while(iter.hasNext()){
            System.out.println(iter.next());
}
    }

    @Test
	public void test_adding_shared_nodes_with_updated_properties() throws DuplicateRecordException, RecordNotFoundException, EncryptionException, AuditFailedException, LabelCannotBeNullException {

		// Add a new entity
		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String label = registryDao.addEntity(graph, String.format("_:%s", rootLabel));
        Graph entity0 = registryDao.getEntityById(label);
        checkIfAuditRecordsAreRight(entity0, null);
		// Create a new TinkerGraph with the existing jsonld
		Graph newEntityGraph = TinkerGraph.open();
		Model newRdfModel = getNewValidRdf();
		updateNodeLabel(newRdfModel, "http://example.com/voc/teacher/1.0.0/IndianUrbanPostalAddress");
		removeStatementFromModel(newRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/municipality"));

		String addressLabel = databaseProvider.getGraphStore().traversal().clone().V()
				.has(T.label, "http://example.com/voc/teacher/1.0.0/IndianUrbanPostalAddress")
				.next().vertices(Direction.IN).next().label();

		// Add a new property to the existing address node
		Resource resource = ResourceFactory.createResource(addressLabel);
		Property predicate = ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/municipality");
		Literal literal = ResourceFactory.createPlainLiteral("Updated MCG");
		newRdfModel.add(ResourceFactory.createStatement(resource, predicate, literal));
		String newRootLabel = updateGraphFromRdf(newRdfModel, newEntityGraph);

		String newEntityResponse = registryDao.addEntity(newEntityGraph, String.format("_:%s", newRootLabel));
		Graph entity = registryDao.getEntityById(newEntityResponse);
		Map<String,Map<String,Integer>> updateCountMap = new HashMap<>();
        Map<String,Integer> updatePropCountMap = new HashMap<>();
        updatePropCountMap.put("http://example.com/voc/teacher/1.0.0/municipality",1);
		updateCountMap.put(addressLabel, updatePropCountMap);
        checkIfAuditRecordsAreRight(entity,updateCountMap);
		String propertyValue = (String) entity.traversal().clone().V()
				.properties("http://example.com/voc/teacher/1.0.0/municipality")
				.next()
				.value();
		assertEquals("Updated MCG", propertyValue);
	}
	
	public void closeDB() throws Exception {
		databaseProvider.shutdown();
	}
	
	@Test
	public void test_read_with_no_data() throws RecordNotFoundException, EncryptionException, AuditFailedException {
		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);
		UUID label = getLabel();
		registryDao.getEntityById(label.toString());
	}

	@Test
	public void test_read_with_some_other_data()
			throws IOException, DuplicateRecordException, RecordNotFoundException, EncryptionException, Exception {

		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);
		String label1 = UUID.randomUUID().toString();
		String label2 = UUID.randomUUID().toString();
		getVertexForSubject(label1, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		registryDao.addEntity(graph, label1);
		registryDao.getEntityById(label2);
	}

	private void dump_graph(Graph g,String filename) throws IOException {
		g.io(IoCore.graphson()).writeGraph(filename);
	}

	@Test
	public void test_read_single_node()
            throws RecordNotFoundException, DuplicateRecordException, IOException, EncryptionException, AuditFailedException {

		String label = getLabel().toString();
		getVertexForSubject(label, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		String response = registryDao.addEntity(graph, label);
		Graph entity = registryDao.getEntityById(response);
		assertNotNull(entity);
//		TODO Write a better checker
		assertEquals(countGraphVertices(graph), countGraphVertices(entity));
	}

	@Test
	public void test_read_nested_node()
            throws NullPointerException, DuplicateRecordException, RecordNotFoundException, EncryptionException, AuditFailedException {

		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);
		Graph entity = registryDao.getEntityById(response);
		assertNotNull(entity);
		assertEquals(countGraphVertices(graph), countGraphVertices(entity));
	}
	
	@Test
	public void test_count_nested_node_with_first_node_as_blank_node()
            throws NullPointerException, DuplicateRecordException, RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException {

		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdfWithFirstNodeAsBlankNode(rdfModel);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);
		Graph entity = registryDao.getEntityById(response);
		assertNotNull(entity);
		assertEquals(countGraphVertices(graph),countGraphVertices(entity));
	}

	@Test
	public void test_blank_node_count_when_no_blank_node_present()
            throws DuplicateRecordException, RecordNotFoundException, EncryptionException, AuditFailedException {

		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);
		Graph entity = registryDao.getEntityById(response);

		GraphTraversal<Vertex, Vertex> blankNodes = entity.traversal().clone().V().filter(v -> v.get().label().startsWith("_:")).V();
		assertEquals(0, IteratorUtils.count(blankNodes));
	}

	@Test
	public void test_update_when_entity_not_exists()
            throws DuplicateRecordException, RecordNotFoundException, EncryptionException, NoSuchElementException, AuditFailedException {

		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		registryDao.addEntity(graph, rootLabel);

		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);

		Graph testGraph = TinkerGraph.open();
		UUID label = getLabel();
		testGraph.addVertex(T.label, label.toString(), "test_label_predicate", "test_value");
		registryDao.updateEntity(testGraph, label.toString(),"addOrUpdate");
	}

	@Test
	public void test_update_single_literal_node()
            throws DuplicateRecordException, RecordNotFoundException, EncryptionException, NoSuchElementException, AuditFailedException, LabelCannotBeNullException {

		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);
        Graph entity = registryDao.getEntityById(response);
		checkIfAuditRecordsAreRight(entity,null);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", response);
		removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"));
		removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"));
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/School");
		Graph updateGraph = TinkerGraph.open();
        createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, response, "addOrUpdate");

		Graph updatedGraphResult = registryDao.getEntityById(response);

        Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(rdfModel, updateRdfModel,Arrays.asList());
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType));

		StringBuilder result = new StringBuilder();
		updatedGraphResult.traversal().V()
				.properties("http://example.com/voc/teacher/1.0.0/revenueBlock")
				.forEachRemaining(p -> result.append(p.value()));
		assertEquals("updated block", result.toString());
	}

    private Model getModelwithOnlyUpdateFacts(Model rdfModel, Model updateRdfModel, List<String> predicatedToExclude) {
        Property propertyToRemove;
        propertyToRemove=ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        Model updateRdfModelWithoutType = restrictModel(updateRdfModel,propertyToRemove);
        Iterator<String> iter = predicatedToExclude.iterator();
        while(iter.hasNext()){
            propertyToRemove= ResourceFactory.createProperty(iter.next());
            updateRdfModelWithoutType = restrictModel(updateRdfModelWithoutType,propertyToRemove);
        }
        System.out.println("ORIGINAL FACTS are");
        printModel(rdfModel);
        System.out.println("FACTS getting updates are: ");
        printModel(updateRdfModelWithoutType);
        return updateRdfModelWithoutType;
    }

    private Model restrictModel(Model updateRdfModel,Property property) {
	    System.out.println("Removing "+property);
        return updateRdfModel.difference(
                    updateRdfModel.listStatements(
                            null,
                            property,
                            (RDFNode) null)
                            .toModel());
    }

    private Map<String, Map<String, Integer>> generateUpdateMapFromRDF(Model updateRdfModelWithoutType) {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        Iterator<Statement> iter = updateRdfModelWithoutType.listStatements();
        while(iter.hasNext()){
            Statement statement = iter.next();
            String subject = statement.getSubject().toString();
            String predicate = statement.getPredicate().toString();
            if(map.containsKey(subject)){
                Map<String, Integer> innerMap = map.get(subject);
                if(innerMap.containsKey(predicate)){
                    int currentCount = innerMap.get(predicate);
                    innerMap.put(predicate,currentCount+1);
                } else {
                    innerMap.put(predicate,1);
                }
            } else {
                Map<String, Integer> innerMap = setInnerMap(predicate);
                map.put(subject,innerMap);
            }
        }
        System.out.println("generateUpdateMapFromRDF "+map);
        return map;
    }

    private Map<String, Integer> setInnerMap(String predicate) {
        Map<String, Integer> innerMap = new HashMap<>();
        innerMap.put(predicate,1);
        return innerMap;
    }

    @Test
	public void test_update_multiple_literal_nodes()
            throws DuplicateRecordException, RecordNotFoundException, EncryptionException, NoSuchElementException, AuditFailedException, LabelCannotBeNullException {

		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", response);
		removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"));
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/School");
		Graph updateGraph = TinkerGraph.open();

		createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, response, "addOrUpdate");

		Graph updatedGraphResult = registryDao.getEntityById(response);

        Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(rdfModel, updateRdfModel,Arrays.asList());
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType));

		ArrayList<String> result = new ArrayList<>();
		String prefix = "http://example.com/voc/teacher/1.0.0";

		updatedGraphResult.traversal().V()
				.properties(String.format("%s/revenueBlock", prefix),
						String.format("%s/clusterResourceCentre", prefix))
				.forEachRemaining(p -> result.add(p.value().toString()));
		List<String> expected = Arrays.asList("updated block", "Updated Cluster Resource");
		assertEquals(5, IteratorUtils.count(updatedGraphResult.traversal().clone().V()));
		assertThat(result, is(expected));
		assertThat(result, contains("updated block", "Updated Cluster Resource"));
		assertThat(result, hasSize(2));
	}

	@Test
	public void test_update_iri_node()
            throws DuplicateRecordException, RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException, LabelCannotBeNullException {

		Model rdfModel = getNewValidRdf();
		Graph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", rootLabel);
		// Remove literal properties from update
		removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"));
		removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/revenueBlock"));
		// Update new rdf model with the labels generated for School and Address nodes
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/School");
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/IndianUrbanPostalAddress");

		// Call update entity
		Graph updateGraph = TinkerGraph.open();
		createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, response, "addOrUpdate");
		Graph updatedGraphResult = registryDao.getEntityById(response);

        Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(rdfModel, updateRdfModel,Arrays.asList("http://example.com/voc/teacher/1.0.0/address"));
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType));

		ArrayList<String> result = new ArrayList<>();
		String prefix = "http://example.com/voc/teacher/1.0.0";

		updatedGraphResult.traversal().clone().V()
				.properties(String.format("%s/mohalla", prefix),
						String.format("%s/municipality", prefix),
						String.format("%s/city", prefix))
				.forEachRemaining(p -> result.add(p.value().toString()));

		List<String> expected = Arrays.asList("Updated Sector 14", "Gurgaon", "Updated MCG");
		assertEquals(5, IteratorUtils.count(updatedGraphResult.traversal().clone().V()));
		assertThat(result, is(expected));
		assertThat(result, contains("Updated Sector 14", "Gurgaon", "Updated MCG"));
		assertThat(result, hasSize(3));
	}

	@Test
	public void test_update_iri_node_and_literal_nodes()
            throws DuplicateRecordException, RecordNotFoundException, EncryptionException, NoSuchElementException, AuditFailedException, LabelCannotBeNullException {

		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", rootLabel);
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/School");
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/IndianUrbanPostalAddress");

		Graph updateGraph = TinkerGraph.open();
		createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, response, "addOrUpdate");

		Graph updatedGraphResult = registryDao.getEntityById(response);

        Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(rdfModel, updateRdfModel,Arrays.asList("http://example.com/voc/teacher/1.0.0/address"));
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType));

		ArrayList<String> result = new ArrayList<>();
		String prefix = "http://example.com/voc/teacher/1.0.0";

		updatedGraphResult.traversal().V()
				.properties(String.format("%s/revenueBlock", prefix),
						String.format("%s/clusterResourceCentre", prefix),
						String.format("%s/mohalla", prefix),
						String.format("%s/municipality", prefix))
				.forEachRemaining(p -> result.add(p.value().toString()));

		List<String> expected = Arrays.asList("updated block", "Updated Cluster Resource", "Updated Sector 14", "Updated MCG");
		assertEquals(5, IteratorUtils.count(updatedGraphResult.traversal().clone().V()));
		assertThat(result, is(expected));
		assertThat(result, contains("updated block", "Updated Cluster Resource", "Updated Sector 14", "Updated MCG"));
		assertThat(result, hasSize(4));
	}
		
	
/*		@Test
		public void testGetEntity(){
			EntityDto entityDto = new EntityDto();
			entityDto.setId(identifier);
			Object entity = registryDao.getEntityById(entityDto);
			assertFalse((entity!=null));
		}
		@Test
		public void testGetNonExistingEntity(){
			EntityDto entityDto = new EntityDto();
			entityDto.setId(generateRandomId());
			Object entity = registryDao.getEntityById(entityDto);
			assertFalse((entity!=null));
		}
		@Test
		public void testModifyEntity(){
			Vertex vertex = graph.addVertex(
					T.label,"identifier");
			vertex.property("is", "108115c3-320c-43d6-aaa7-7aab72777575");
			graph.addVertex(
					T.label,"type").property("is", "teacher");
			getVertexForSubject("identifier","is", identifier, t);
			getVertexForSubject("type","is", "teacher", t);
			getVertexForSubject("email","is", "consent driven");
			boolean response = registryDao.updateEntity(graph);
			assertFalse(response);
		}
*/
	

	@After
	public void shutDown() throws Exception{
		if(graph!=null){
			graph.close();
		}
	}

	@Test
	public void savingMetaProperties() throws DuplicateRecordException, RecordNotFoundException, EncryptionException, AuditFailedException {
        Model inputModel = getNewValidRdf(RICH_LITERAL_TTL, "ex:");
        String rootLabel = updateGraphFromRdf(inputModel, graph, "http://example.org/typeProperty");
        registryDao.addEntity(graph, rootLabel);
        Graph entity = registryDao.getEntityById(rootLabel);
        org.eclipse.rdf4j.model.Model model = RDF2Graph.convertGraph2RDFModel(entity, rootLabel);
        Model outputModel = JenaRDF4J.asJenaModel(model);
        assertTrue(inputModel.difference(outputModel).isEmpty());
        assertTrue(outputModel.difference(inputModel).isEmpty());

	}

	private void updateNodeLabel(Model rdfModel, String nodeLabel) {
		String labelForUpdate = databaseProvider.getGraphStore().traversal().clone().V()
				.has(T.label, nodeLabel)
				.next().vertices(Direction.IN).next().label();
		RDFUtil.updateRdfModelNodeId(rdfModel,
				ResourceFactory.createResource(nodeLabel), labelForUpdate);
	}

	private String getJsonldFromGraph(Graph entity, String rootLabel) {
		org.eclipse.rdf4j.model.Model model = RDF2Graph.convertGraph2RDFModel(entity, rootLabel);
		String jsonldOutput = "";
		try {
			jsonldOutput = RDFUtil.frameEntity(model);
		} catch (IOException ex) {
			System.out.println("IO Exception = " + ex);
		}
		return jsonldOutput;
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
	
	public Vertex getVertexWithMultipleProperties(String subjectValue, Map<String, String> map){
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
		for (Map.Entry<String, String> entry : map.entrySet())
		{
			vertex.property(entry.getKey(), entry.getValue());
		}
		return vertex;
	}
	
	private void removeStatementFromModel(Model rdfModel, Property predicate) {
		StmtIterator blankNodeIterator =
				rdfModel.listStatements(null, predicate, (RDFNode) null);

		// Remove all the blank nodes from the existing model to create test data
		while(blankNodeIterator.hasNext()) {
			Statement parentStatement = blankNodeIterator.next();
			if(parentStatement.getObject() instanceof Resource) {
				StmtIterator childStatements = rdfModel.listStatements((Resource) parentStatement.getObject(), null, (RDFNode) null);
				while (childStatements.hasNext()) {
					childStatements.next();
					childStatements.remove();
				}
			}
			blankNodeIterator.remove();
		}
	}

	private Model createRdfFromFile(String jsonldFilename, String rootNodeLabel) {
		return getNewValidRdf(jsonldFilename, CONTEXT_CONSTANT, rootNodeLabel);
	}

   private String createGraphFromRdf(Graph newGraph, Model rdfModel) {
		StmtIterator iterator = rdfModel.listStatements();
		boolean rootSubjectFound = false;
		String label = null;
		while (iterator.hasNext()) {
			Statement rdfStatement = iterator.nextStatement();
			if (!rootSubjectFound) {
				String type = environment.getProperty(Constants.SUBJECT_LABEL_TYPE);
				label = RDF2Graph.getRootSubjectLabel(rdfStatement, type);
				if (label != null) {
					rootSubjectFound = true;
				}
			}
			org.eclipse.rdf4j.model.Statement rdf4jStatement = JenaRDF4J.asrdf4jStatement(rdfStatement);
			newGraph = RDF2Graph.convertRDFStatement2Graph(rdf4jStatement, newGraph);
		}
		return label;
	}
	
	
	private String updateGraphFromRdfWithFirstNodeAsBlankNode(Model rdfModel){
		StmtIterator iterator = rdfModel.listStatements();
		StmtIterator iterator2 = rdfModel.listStatements();
		boolean rootSubjectFound = false;
		String label = null;
		while(iterator2.hasNext()){
			Statement rdfStatement = iterator2.nextStatement();
			org.eclipse.rdf4j.model.Statement rdf4jStatement = JenaRDF4J.asrdf4jStatement(rdfStatement);
			String subjectValue = rdf4jStatement.getSubject().toString();
			String predicate = rdf4jStatement.getPredicate().toString();
			if(subjectValue.startsWith("_:") && predicate.equals(RDF.TYPE.toString())){
				graph = RDF2Graph.convertRDFStatement2Graph(rdf4jStatement, graph);
				break;
			}
		}
		while(iterator.hasNext()){
			Statement rdfStatement = iterator.nextStatement();
			if(!rootSubjectFound){
				String type = environment.getProperty(Constants.SUBJECT_LABEL_TYPE);
				label = RDF2Graph.getRootSubjectLabel(rdfStatement,type);
				if(label!=null){
					rootSubjectFound = true;
				}
			}
			org.eclipse.rdf4j.model.Statement rdf4jStatement = JenaRDF4J.asrdf4jStatement(rdfStatement);
			String subjectValue = rdf4jStatement.getSubject().toString();
			String predicate = rdf4jStatement.getPredicate().toString();
			if(subjectValue.startsWith("_:")&& predicate.equals(RDF.TYPE.toString())){
				continue;
			}
			graph = RDF2Graph.convertRDFStatement2Graph(rdf4jStatement, graph);
	}
		return label;
	}

	private long countGraphVertices(Graph graph) {
		return IteratorUtils.count(graph.vertices());
	}
		
	@Test
	public void test_encryption_for_nonEncryptableProperty() throws Exception {
		String label = generateRandomId();
		Map<String, String> map = new HashMap<>();
		map.put("http://example.com/voc/teacher/1.0.0/schoolName", "ABC International School");
		map.put("http://example.com/voc/teacher/1.0.0/clusterResourceCentre", "test Cluster Resource");
		map.put("http://example.com/voc/teacher/1.0.0/udiseNumber", "1234");

		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/schoolName")).thenReturn(false);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"))
				.thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/udiseNumber")).thenReturn(true);

		when(encryptionMock.encrypt("test Cluster Resource")).thenReturn("mockEncryptedClusterResourceCentre");
		when(encryptionMock.encrypt("1234")).thenReturn("mockEncryptedUdiseNumber");

		getVertexWithMultipleProperties(label, map);
		String response = registryDao.addEntity(graph,label);
		registryDao.getEntityById(response);
		verify(encryptionMock, times(2)).encrypt(Mockito.anyString());
	}
	
	@Test
	public void test_encryptionCall_for_encryptable_but_null_property() throws Exception {
		String label = generateRandomId();
		Map<String, String> map = new HashMap<>();
		map.put("http://example.com/voc/teacher/1.0.0/schoolName", "ABC International School");
		map.put("http://example.com/voc/teacher/1.0.0/clusterResourceCentre", "test Cluster Resource");
		map.put("http://example.com/voc/teacher/1.0.0/udiseNumber", "1234");

		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/schoolName")).thenReturn(false);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/clusterResourceCentre")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/udiseNumber")).thenReturn(true);

		when(encryptionMock.encrypt("test Cluster Resource")).thenReturn("mockEncryptedClusterResourceCentre");
		when(encryptionMock.encrypt("1234")).thenReturn("mockEncryptedUdiseNumber");

		getVertexWithMultipleProperties(label, map);
		String response = registryDao.addEntity(graph, label);
		registryDao.getEntityById(response);

		verify(encryptionMock, times(2)).encrypt(Mockito.anyString());
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
		Map<String,String> map=new HashMap<>();
		map.put("http://example.com/voc/teacher/1.0.0/schoolName", "ABC International School");
		map.put("http://example.com/voc/teacher/1.0.0/clusterResourceCentre", "test Cluster Resource");
		map.put("http://example.com/voc/teacher/1.0.0/udiseNumber", "1234");
		
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/schoolName")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/clusterResourceCentre")).thenReturn(true);
	    when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/udiseNumber")).thenReturn(true);
		
	    when(encryptionMock.encrypt("ABC International School")).thenReturn("mockEncryptedSchoolName");
	    when(encryptionMock.encrypt("test Cluster Resource")).thenReturn("mockEncryptedClusterResourceCentre");
	    when(encryptionMock.encrypt("1234")).thenReturn("mockEncryptedUdiseNumber");
	    
	    when(mockSchemaConfigurator.isEncrypted("encryptedschoolName")).thenReturn(true);
		when(mockSchemaConfigurator.isEncrypted("encryptedclusterResourceCentre")).thenReturn(true);
	    when(mockSchemaConfigurator.isEncrypted("encryptedudiseNumber")).thenReturn(true);
	    
	    when(encryptionMock.decrypt("mockEncryptedSchoolName")).thenReturn("ABC International School");
	    when(encryptionMock.decrypt("mockEncryptedClusterResourceCentre")).thenReturn("test Cluster Resource");
	    when(encryptionMock.decrypt("mockEncryptedUdiseNumber")).thenReturn("1234");
		
	    getVertexWithMultipleProperties(label,map);		
		String response = registryDao.addEntity(graph, label);
		registryDao.getEntityById(response);		
		
		verify(encryptionMock, times(3)).encrypt(Mockito.anyString());
		verify(encryptionMock, times(3)).decrypt(Mockito.anyString());
	}
	
	@Test
	public void test_properties_multi_node() throws Exception {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/schoolName")).thenReturn(true);
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/clusterResourceCentre")).thenReturn(true);
	    when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/udiseNumber")).thenReturn(true);
	    
	    when(encryptionMock.encrypt("Bluebells")).thenReturn("mockEncryptedSchoolName");
	    when(encryptionMock.encrypt("some Cluster Resource")).thenReturn("mockEncryptedClusterResourceCentre");
	    when(encryptionMock.encrypt("9876")).thenReturn("mockEncryptedUdiseNumber");
	    
	    when(mockSchemaConfigurator.isEncrypted("encryptedschoolName")).thenReturn(true);
		when(mockSchemaConfigurator.isEncrypted("encryptedclusterResourceCentre")).thenReturn(true);
	    when(mockSchemaConfigurator.isEncrypted("encryptedudiseNumber")).thenReturn(true);
	    
	    when(encryptionMock.decrypt("mockEncryptedSchoolName")).thenReturn("ABC International School");
	    when(encryptionMock.decrypt("mockEncryptedClusterResourceCentre")).thenReturn("test Cluster Resource");
	    when(encryptionMock.decrypt("mockEncryptedUdiseNumber")).thenReturn("1234");
	  
	    String response =registryDao.addEntity(graph, "_:"+rootLabel);					
	    registryDao.getEntityById(response);
	    
		verify(encryptionMock, times(3)).encrypt(Mockito.anyString());	
		verify(encryptionMock, times(3)).decrypt(Mockito.anyString());		
	}
	
	@Test
	public void test_encryption_EncryptionError() throws Exception {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		when(encryptionMock.encrypt(Mockito.anyString())).thenThrow(EncryptionException.class);

		try {
			registryDao.addEntity(graph, rootLabel);
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
			String response = registryDao.addEntity(graph, String.format("_:%s", label));
			registryDao.getEntityById(response);
		} catch (EncryptionException e) {
			assertThat(e.toString(), allOf(containsString("EncryptionException")));
		}
	}

	@Test
	public void test_system_property_should_never_fetched_in_audit_record() throws Exception {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel));
		Graph entity = registryDao.getEntityById(response);
		GraphTraversal<Vertex, Vertex> updatedgraphTraversal = entity.traversal().clone().V();
		while (updatedgraphTraversal.hasNext()) {
			Vertex v = updatedgraphTraversal.next();
			Iterator<VertexProperty<Object>> iter = v.properties();
			while (iter.hasNext()) {
				VertexProperty<Object> property = iter.next();
				Assert.assertThat(property.key(), not(containsString("@")));
			}
		}
	}
}
