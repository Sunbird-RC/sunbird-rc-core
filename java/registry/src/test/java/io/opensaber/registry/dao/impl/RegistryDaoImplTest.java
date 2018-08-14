package io.opensaber.registry.dao.impl;

import io.opensaber.registry.authorization.AuthorizationToken;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.audit.LabelCannotBeNullException;
import io.opensaber.registry.model.AuditRecordReader;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.tests.utility.TestHelper;

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
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import io.opensaber.converters.JenaRDF4J;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.MultipleEntityException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.registry.service.impl.EncryptionServiceImpl;
import io.opensaber.utils.converters.RDF2Graph;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import java.io.IOException;
import java.util.*;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RegistryDaoImpl.class, Environment.class, ObjectMapper.class,
		GenericConfiguration.class, EncryptionServiceImpl.class, AuditRecordReader.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryDaoImplTest extends RegistryTestBase {
	private static Logger logger = LoggerFactory.getLogger(RegistryDaoImplTest.class);
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Autowired
	private Environment environment;
	
	@Autowired
	private RegistryDaoImpl registryDao;
	
  	private static Graph graph;
			
	@Autowired
	private DatabaseProvider databaseProvider;
	
	@Autowired
	AuditRecordReader auditRecordReader;
	
	@Value("${registry.context.base}")
	private String registryContext;

	
    private static final String RICH_LITERAL_TTL = "rich-literal.jsonld";
	private static final String CONTEXT_CONSTANT = "sample:";
	
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
	public void test_adding_a_single_node() throws DuplicateRecordException, RecordNotFoundException, EncryptionException, LabelCannotBeNullException, AuditFailedException {
		String label = generateRandomId();
		getVertexForSubject(label, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		String response = registryDao.addEntity(graph, label,null, null);
		Graph entity = registryDao.getEntityById(response);
		assertEquals(1, IteratorUtils.count(entity.traversal().clone().V()));
		Vertex v = entity.traversal().V().has(T.label, response).next();
		assertEquals("DAV Public School", v.property("http://example.com/voc/teacher/1.0.0/schoolName").value());
        checkIfAuditRecordsAreRight(entity, null);
	}

	private int checkIfAuditRecordsAreRight(Graph entity, Map<String, Map<String, Integer>> updateCountMap) throws LabelCannotBeNullException {
		boolean auditEnabled = environment.getProperty("audit.enabled") != null ? Boolean.parseBoolean(environment.getProperty("audit.enabled")) : false;
		int count = 0;
		if(auditEnabled){
			int adjustedCount;
			Map<String, Integer> pairMap = getPropCounterMap(entity);
			Iterator it = pairMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Integer> pair = (Map.Entry) it.next();
				int updatedPropertyCount = 0;
				if (updateCountMap != null) {
					Map<String, Integer> labelPropertyMap = updateCountMap.get(String.valueOf(pair.getKey()));
					if (labelPropertyMap != null) {
						updatedPropertyCount = labelPropertyMap.values().stream().mapToInt(i -> i).sum();
						;
					}
				}
				adjustedCount = pair.getValue().intValue() + updatedPropertyCount;
				count += adjustedCount;
				assertEquals(adjustedCount, auditRecordReader.fetchAuditRecords(String.valueOf(pair.getKey()), null).size());
				it.remove();
			}
		}
		return count;
	}


    public String updateGraphFromRdf(Model rdfModel) {
		 
        return updateGraphFromRdf(rdfModel, graph);
    }


    private Map<String,Integer> getPropCounterMap(Graph entity) {
	    Map<String,Integer> entityPropertyCountMap = new HashMap<>();
        Iterator<Vertex> iter = entity.traversal().clone().V().hasNot(Constants.AUDIT_KEYWORD);
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

    @Test
	public void test_adding_existing_root_node() throws NullPointerException, DuplicateRecordException, EncryptionException, AuditFailedException, RecordNotFoundException {
		expectedEx.expect(DuplicateRecordException.class);
		expectedEx.expectMessage(Constants.DUPLICATE_RECORD_MESSAGE);
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel), null, null);
		Graph entity = registryDao.getEntityById(response);
		registryDao.addEntity(entity, response, null, null);
	}
	
	@Test
	public void test_adding_multiple_nodes() throws NullPointerException, DuplicateRecordException, RecordNotFoundException, EncryptionException, AuditFailedException, LabelCannotBeNullException {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel), null, null);
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
			String rootLabelEntity1 = updateGraphFromRdf(rdfModel1, graphEntity1);

			String entity1Label = registryDao.addEntity(graphEntity1, "_:" + rootLabelEntity1, null, null);

			Graph entity1 = registryDao.getEntityById(entity1Label);
            checkIfAuditRecordsAreRight(entity1, null);

			// Expected count of vertices in one entity
			assertEquals(5, IteratorUtils.count(entity1.traversal().V()));

			Model rdfModel2 = getNewValidRdf();
			TinkerGraph graphEntity2 = TinkerGraph.open();
			String rootLabelEntity2 = updateGraphFromRdf(rdfModel2, graphEntity2);
			String entity2Label = registryDao.addEntity(graphEntity2, "_:" + rootLabelEntity2, null, null);

			Graph entity2 = registryDao.getEntityById(entity2Label);
            checkIfAuditRecordsAreRight(entity2, null);

			assertEquals(5, IteratorUtils.count(entity2.traversal().V()));
			long verticesCountAfterSharedNodesCreation =
					IteratorUtils.count(databaseProvider.getGraphStore().traversal().clone().V()
							.filter(v -> !v.get().label().equalsIgnoreCase(Constants.GRAPH_GLOBAL_CONFIG))
							.hasNot(Constants.AUDIT_KEYWORD));
			long edgesCountAfterSharedNodesCreation =
					IteratorUtils.count(databaseProvider.getGraphStore().traversal().clone().E()
							.hasNot(Constants.AUDIT_KEYWORD));

			// Expected count of vertices is 6 with two entities with same address created
			assertEquals(7, verticesCountAfterSharedNodesCreation);
			assertEquals(8, edgesCountAfterSharedNodesCreation);

		} catch (DuplicateRecordException | RecordNotFoundException | EncryptionException | NoSuchElementException e) {
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
		logger.debug("-------- MODEL TO ADD----------");
        printModel(rdfModel);
        String newEntityResponse;
        newEntityResponse = registryDao.addEntity(graph, String.format("_:%s", rootLabel), null, null);
        Graph entity = registryDao.getEntityById(newEntityResponse);
        logger.debug("-------- CHECKING AUDIT RECORDS-------");
        int count1 = checkIfAuditRecordsAreRight(entity, null);
        logger.debug("--------- AUDIT RECORDS -------"+count1);
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
		newEntityResponse = registryDao.addEntity(newEntityGraph, String.format("_:%s", newRootLabel), null, null);
		entity = registryDao.getEntityById(newEntityResponse);
        int count2 = checkIfAuditRecordsAreRight(entity, null);
        logger.debug("------- AUDIT RECORDS ------"+count2);
		String propertyValue = (String) entity.traversal().clone().V()
			.properties("http://example.com/voc/teacher/1.0.0/districtAlias")
			.next()
			.value();
		assertEquals("Gurgaon alias", propertyValue);
		boolean auditEnabled = environment.getProperty("audit.enabled") != null ? Boolean.parseBoolean(environment.getProperty("audit.enabled")) : false;
		if(auditEnabled){
			assertEquals(1,count2-count1);
		}

	}

    private void printModel(Model rdfModel) {
        Iterator iter = rdfModel.listStatements();
        while(iter.hasNext()){
            logger.debug("-------next iterator in printModel() : {} ",iter.next());
}
    }

    @Test
	public void test_adding_shared_nodes_with_updated_properties() throws DuplicateRecordException, RecordNotFoundException, EncryptionException, AuditFailedException, LabelCannotBeNullException {

		// Add a new entity
		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String label = registryDao.addEntity(graph, String.format("_:%s", rootLabel), null, null);
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

		String newEntityResponse = registryDao.addEntity(newEntityGraph, String.format("_:%s", newRootLabel), null, null);
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
		registryDao.addEntity(graph, label1, null, null);
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
		String response = registryDao.addEntity(graph, label, null, null);
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
		String response = registryDao.addEntity(graph, "_:"+rootLabel, null, null);
		Graph entity = registryDao.getEntityById(response);
		assertNotNull(entity);
		assertEquals(countGraphVertices(graph), countGraphVertices(entity));
	}
	
	@Test
	public void test_count_nested_node_with_first_node_as_blank_node()
            throws NullPointerException, DuplicateRecordException, RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException {

		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdfWithFirstNodeAsBlankNode(rdfModel);
		String response = registryDao.addEntity(graph, "_:"+rootLabel, null, null);
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
		String response = registryDao.addEntity(graph, "_:"+rootLabel, null, null);
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
		registryDao.addEntity(graph, rootLabel, null, null);

		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);

		Graph testGraph = TinkerGraph.open();
		UUID label = getLabel();
		testGraph.addVertex(T.label, label.toString(), "test_label_predicate", "test_value");
		registryDao.updateEntity(testGraph, label.toString(),"update");
	}

	@Test
	public void test_update_single_literal_node()
            throws DuplicateRecordException, RecordNotFoundException, EncryptionException, NoSuchElementException, AuditFailedException, LabelCannotBeNullException {

		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel, null, null);
        Graph entity = registryDao.getEntityById(response);
		checkIfAuditRecordsAreRight(entity,null);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", response);
		/*removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"));
		removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"));
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/School");*/
		Graph updateGraph = TinkerGraph.open();
        //createGraphFromRdf(updateGraph, updateRdfModel);
		updateGraphFromRdf(updateRdfModel, updateGraph);
		registryDao.updateEntity(updateGraph, response, "update");

		Graph updatedGraphResult = registryDao.getEntityById(response);

        /*Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(rdfModel, updateRdfModel,Arrays.asList());
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType));*/
		Model addedModel = JenaRDF4J.asJenaModel(RDF2Graph.convertGraph2RDFModel(entity, response));
        Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(addedModel, updateRdfModel,Arrays.asList());
        Model deletedFacts = getModelwithDeletedFacts(addedModel, updateRdfModel, updateRdfModelWithoutType);
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType, deletedFacts));

		StringBuilder result = new StringBuilder();
		updatedGraphResult.traversal().V()
				.properties("http://example.com/voc/teacher/1.0.0/revenueBlock")
				.forEachRemaining(p -> result.append(p.value()));
		assertEquals("updated block", result.toString());
	}

    private Model getModelwithOnlyUpdateFacts(Model rdfModel, Model updateRdfModel, List<String> predicatedToExclude) {
    	Model updatedFacts = updateRdfModel.difference(rdfModel);
    	logger.debug("-------- UPDATED FACTS are: ----------"+updatedFacts);
       /* Property propertyToRemove;
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
        printModel(updateRdfModelWithoutType);*/
        return updatedFacts;
    }
    
    private Model getModelwithDeletedFacts(Model rdfModel, Model updateRdfModel, Model updatedFactsModel) {
    	Model getModelDiff = rdfModel.difference(updateRdfModel);
    	Model deletedFacts = ModelFactory.createDefaultModel();
    	StmtIterator stmtIter = updatedFactsModel.listStatements();
    	Map<String, String> encounteredSubPred = new HashMap<String, String>();
    	while(stmtIter.hasNext()){
    		Statement s = stmtIter.next();
    		Resource  subject = s.getSubject();
    		Property predicate = s.getPredicate();
    		if(!encounteredSubPred.containsKey(subject) || (encounteredSubPred.containsKey(subject) && 
    				!encounteredSubPred.get(subject).equalsIgnoreCase(predicate.toString()))){
    			encounteredSubPred.put(subject.toString(), predicate.toString());
    			StmtIterator diffIter = getModelDiff.listStatements(subject, predicate, (RDFNode)null);
    			while(diffIter.hasNext()){
    				Statement diffStatement = diffIter.next();
    				if(diffStatement.getObject().isResource()){
    					deletedFacts.add(diffStatement);
    				}
    			}
    		}
    	}
    	logger.debug("------- DELETED FACTS are: --------"+deletedFacts);
        return deletedFacts;
    }

    private Model restrictModel(Model updateRdfModel,Property property) {
	    logger.debug("--------Removing property :  "+property);
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
        logger.debug("generateUpdateMapFromRDF "+map);
        return map;
    }

    private Map<String, Map<String, Integer>> generateUpdateMapFromRDF(Model updateRdfModelWithoutType, Model deletedFacts) {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        Iterator<Statement> iter = updateRdfModelWithoutType.listStatements();
        Map<String, String> encounteredSubPred = new HashMap<String, String>();
        while(iter.hasNext()){
            Statement statement = iter.next();
            Resource subject = statement.getSubject();
            Property predicate = statement.getPredicate();
            int deletedCount = 0;
            if(!encounteredSubPred.containsKey(subject) || (encounteredSubPred.containsKey(subject) && 
    				!encounteredSubPred.get(subject).equalsIgnoreCase(predicate.toString()))){
            	encounteredSubPred.put(subject.toString(), predicate.toString());
    			StmtIterator diffIter = deletedFacts.listStatements(subject, predicate, (RDFNode)null);
    			deletedCount = (int)IteratorUtils.count(diffIter);
    			logger.debug(String.format("Number of facts deleted for %s and %s : %d",subject.toString(), predicate.toString(),deletedCount));
            }
            if(map.containsKey(subject.toString())){
                Map<String, Integer> innerMap = map.get(subject.toString());
                if(innerMap.containsKey(predicate)){
                    int currentCount = innerMap.get(predicate.toString());
                    innerMap.put(predicate.toString(),currentCount+1+deletedCount);
                } else {
                    innerMap.put(predicate.toString(),1+deletedCount);
                }
            } else {
                Map<String, Integer> innerMap = setInnerMap(predicate.toString(), deletedCount);
                map.put(subject.toString(),innerMap);
            }
        }
       logger.debug("generateUpdateMapFromRDF "+map);
        return map;
    }

    private Map<String, Integer> setInnerMap(String predicate, int deletedCount) {
        Map<String, Integer> innerMap = new HashMap<>();
        innerMap.put(predicate,1+deletedCount);
        return innerMap;
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
		String response = registryDao.addEntity(graph, "_:"+rootLabel, null, null);
		Graph entity = registryDao.getEntityById(response);
		Model updateRdfModel = createRdfFromFile("update_node.jsonld", response);
		removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"));
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/School");
		Graph updateGraph = TinkerGraph.open();

		//createGraphFromRdf(updateGraph, updateRdfModel);
		updateGraphFromRdf(updateRdfModel, updateGraph);
		registryDao.updateEntity(updateGraph, response, "update");

		Graph updatedGraphResult = registryDao.getEntityById(response);

        /*Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(rdfModel, updateRdfModel,Arrays.asList());
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType));*/
		Model addedModel = JenaRDF4J.asJenaModel(RDF2Graph.convertGraph2RDFModel(entity, response));
        Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(addedModel, updateRdfModel,Arrays.asList());
        Model deletedFacts = getModelwithDeletedFacts(addedModel, updateRdfModel, updateRdfModelWithoutType);
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType, deletedFacts));

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
	public void test_add_iri_node_to_existing_entity()
            throws DuplicateRecordException, RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException, LabelCannotBeNullException,
            MultipleEntityException, EntityCreationException{

		Model rdfModel = getNewValidRdf();
		Graph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel, null, null);
		Graph entity = registryDao.getEntityById(response);
		Model updateRdfModel = getNewValidRdf("add_node.jsonld");

		// Call add entity
		Graph updateGraph = TinkerGraph.open();
		String label = getRootLabel(updateRdfModel);
		generateGraphFromRDF(updateGraph, updateRdfModel);
		String newResponse = registryDao.addEntity(updateGraph,label, response, "http://example.com/voc/teacher/1.0.0/address");
		Graph newUpdatedGraphResult = registryDao.getEntityById(newResponse);
		assertEquals(2, IteratorUtils.count(newUpdatedGraphResult.traversal().clone().V()));
		Graph updatedGraphResult = registryDao.getEntityById(response);
        /*Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(rdfModel, updateRdfModel,Arrays.asList("http://example.com/voc/teacher/1.0.0/address"));
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType));*/
		Model addedModel = JenaRDF4J.asJenaModel(RDF2Graph.convertGraph2RDFModel(entity, response));
        Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(addedModel, updateRdfModel,Arrays.asList());
        Model deletedFacts = getModelwithDeletedFacts(addedModel, updateRdfModel, updateRdfModelWithoutType);
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType, deletedFacts));


		ArrayList<String> result = new ArrayList<>();
		String prefix = "http://example.com/voc/teacher/1.0.0";

		updatedGraphResult.traversal().clone().V()
				.properties(String.format("%s/mohalla", prefix),
						String.format("%s/municipality", prefix),
						String.format("%s/city", prefix))
				.forEachRemaining(p -> result.add(p.value().toString()));

		List<String> expected = Arrays.asList("Updated Sector 14", "Updated MCG", "Sector 14", "Gurgaon","MCG");
		assertEquals(7, IteratorUtils.count(updatedGraphResult.traversal().clone().V()));
		assertThat(result, is(expected));
		//assertThat(result, contains("Updated Sector 14", "Gurgaon", "Updated MCG"));
		assertThat(result, hasSize(5));
	}

	@Test
	public void test_update_iri_node_and_literal_nodes()
            throws DuplicateRecordException, RecordNotFoundException, EncryptionException, NoSuchElementException,
            AuditFailedException, LabelCannotBeNullException, MultipleEntityException, EntityCreationException {

		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel, null, null);
		Graph entity = registryDao.getEntityById(response);
		Model addedModel = JenaRDF4J.asJenaModel(RDF2Graph.convertGraph2RDFModel(entity, response));
		Model updateRdfModel = createRdfFromFile("update_node.jsonld", response);
		StmtIterator stmt = addedModel.listStatements(ResourceFactory.createResource(response), ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"),(RDFNode)null);
		while(stmt.hasNext()){
			Statement s = stmt.next();
			Resource subject = s.getSubject();
			Property predicate = s.getPredicate();
			if(subject.toString().equals(response) && predicate.toString().equalsIgnoreCase("http://example.com/voc/teacher/1.0.0/address")){
			RDFNode rdfNode = s.getObject();
			updateRdfModel.add(s);
			updateRdfModel.add((Resource)rdfNode, 
					ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/mohalla"), "Updated Sector 14");
			updateRdfModel.add((Resource)rdfNode, 
					ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/municipality"), "Updated MCG");
			}
		}

		Graph updateGraph = TinkerGraph.open();
		//String label = getRootLabel(updateRdfModel);
		generateGraphFromRDF(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, response, "update");

		Graph updatedGraphResult = registryDao.getEntityById(response);

        /*Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(rdfModel, updateRdfModel,Arrays.asList("http://example.com/voc/teacher/1.0.0/address"));
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType));*/
		
        Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(addedModel, updateRdfModel,Arrays.asList());
        Model deletedFacts = getModelwithDeletedFacts(addedModel, updateRdfModel, updateRdfModelWithoutType);
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType, deletedFacts));

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
					T.label,"mapType").property("is", "teacher");
			getVertexForSubject("identifier","is", identifier, t);
			getVertexForSubject("mapType","is", "teacher", t);
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
        String rootLabel = updateGraphFromRdf(inputModel, graph);
        registryDao.addEntity(graph, rootLabel, null, null);
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
			logger.debug("IO Exception = " + ex);
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

	private Model createRdfFromFile(String jsonldFilename, String rootNodeLabel) {
		return getNewValidRdf(jsonldFilename, CONTEXT_CONSTANT, rootNodeLabel);
	}

   	
   private String getRootLabel(Model entity) throws EntityCreationException, MultipleEntityException{
		List<Resource> rootLabels = RDFUtil.getRootLabels(entity);
		if(rootLabels.size() == 0){
			throw new EntityCreationException(Constants.NO_ENTITY_AVAILABLE_MESSAGE);
		} else if(rootLabels.size() > 1){
			throw new MultipleEntityException(Constants.ADD_UPDATE_MULTIPLE_ENTITIES_MESSAGE);
		} else{
			Resource subject = rootLabels.get(0);
			String label = subject.toString();
			if(subject.isAnon() && subject.getURI() == null){
				label = String.format("_:%s", label);
			}
			return label;
		}
	}
	
	
	
	
	private String updateGraphFromRdfWithFirstNodeAsBlankNode(Model rdfModel){
		StmtIterator iterator = rdfModel.listStatements();
		StmtIterator iterator2 = rdfModel.listStatements();
		List<Resource> resList = RDFUtil.getRootLabels(rdfModel);
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
			org.eclipse.rdf4j.model.Statement rdf4jStatement = JenaRDF4J.asrdf4jStatement(rdfStatement);
			String subjectValue = rdf4jStatement.getSubject().toString();
			String predicate = rdf4jStatement.getPredicate().toString();
			if(subjectValue.startsWith("_:")&& predicate.equals(RDF.TYPE.toString())){
				continue;
			}
			graph = RDF2Graph.convertRDFStatement2Graph(rdf4jStatement, graph);
	}
		return resList.get(0).toString();
	}

	private long countGraphVertices(Graph graph) {
		return IteratorUtils.count(graph.vertices());
	}
		
	@Test
	public void test_system_property_should_never_fetched_in_audit_record() throws Exception {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel), null, null);
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
	
	/*@Test
	public void test_delete_non_existing_root_node() throws Exception {
		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);
		String id = generateRandomId();
		Model rdfModel = ModelFactory.createDefaultModel();
		rdfModel.add(ResourceFactory.createResource(id), null, (RDFNode)null);
		String rootLabel = updateGraphFromRdf(rdfModel);
		//registryDao.deleteEntity(graph,rootLabel);
	}
	
	@Test
	public void test_delete_non_existing_nested_node() throws Exception {
		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel));
		//Graph entity = registryDao.getEntityById(response);
		String id = generateRandomId();
		Model rdfDeleteModel = ModelFactory.createDefaultModel();
		rdfDeleteModel.add(ResourceFactory.createResource(response), null, (RDFNode)ResourceFactory.createResource(id));
		rootLabel = updateGraphFromRdf(rdfDeleteModel);
		//registryDao.deleteEntity(graph,response);
	}
	
	@Test
	public void test_delete_existing_node() throws Exception {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel));
		Model rdfDeleteModel = ModelFactory.createDefaultModel();
		rdfDeleteModel.add(ResourceFactory.createResource(response), 
				ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/area"), 
				(RDFNode)ResourceFactory.createResource("http://example.com/voc/teacher/1.0.0/AreaTypeCode-URBAN"));
		rootLabel = updateGraphFromRdf(rdfDeleteModel);
		//registryDao.deleteEntity(graph,response);
		Graph updatedGraphResult = registryDao.getEntityById(response);
		assertFalse(updatedGraphResult.traversal().E().hasLabel("http://example.com/voc/teacher/1.0.0/area").hasNext());
	}*/
	
	@Test
	public void test_update_properties() throws Exception {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel), null, null);
		Graph entity = registryDao.getEntityById(response);
		checkIfAuditRecordsAreRight(entity,null);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", response);
		removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"));
		/*removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"));
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/School");*/
		Graph updateGraph = TinkerGraph.open();
		updateGraph = generateGraphFromRDF(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, response, "update");

		Graph updatedGraphResult = registryDao.getEntityById(response);

        /*Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(rdfModel, updateRdfModel,Arrays.asList());
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType));*/
		Model addedModel = JenaRDF4J.asJenaModel(RDF2Graph.convertGraph2RDFModel(entity, response));
        Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(addedModel, updateRdfModel,Arrays.asList());
        Model deletedFacts = getModelwithDeletedFacts(addedModel, updateRdfModel, updateRdfModelWithoutType);
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType, deletedFacts));

		StringBuilder result = new StringBuilder();
		updatedGraphResult.traversal().V()
				.properties("http://example.com/voc/teacher/1.0.0/revenueBlock")
				.forEachRemaining(p -> result.append(p.value()));
		assertEquals("updated block", result.toString());
	}
	
	@Test
	public void test_update_single_valued_properties() throws Exception {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel), null, null);
		Graph entity = registryDao.getEntityById(response);
		
		checkIfAuditRecordsAreRight(entity,null);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", response);
		removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"));
		removeStatementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/revenueBlock"));
		Resource newSubject = ResourceFactory.createResource(response);
		Property newPredicate = ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/area");
		RDFNode newRdfNode = ResourceFactory.createResource("http://example.com/voc/teacher/1.0.0/AreaTypeCode-RURAL");
		updateRdfModel.add(newSubject, newPredicate, newRdfNode);
		Graph updateGraph = TinkerGraph.open();
		updateGraph = generateGraphFromRDF(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, response, "update");

		Graph updatedGraphResult = registryDao.getEntityById(response);
		Model addedModel = JenaRDF4J.asJenaModel(RDF2Graph.convertGraph2RDFModel(entity, response));
        Model updateRdfModelWithoutType = getModelwithOnlyUpdateFacts(addedModel, updateRdfModel,Arrays.asList());
        Model deletedFacts = getModelwithDeletedFacts(addedModel, updateRdfModel, updateRdfModelWithoutType);
        checkIfAuditRecordsAreRight(updatedGraphResult,generateUpdateMapFromRDF(updateRdfModelWithoutType, deletedFacts));

		assertTrue(updatedGraphResult.traversal().E().hasLabel("http://example.com/voc/teacher/1.0.0/area").hasNext());
		assertEquals(updatedGraphResult.traversal().E().hasLabel("http://example.com/voc/teacher/1.0.0/area").next().inVertex().label(),"http://example.com/voc/teacher/1.0.0/AreaTypeCode-RURAL");
	}
	
	@Test
	public void test_setAuthInfo_for_create(){
		Graph graph = TinkerGraph.open();
		Vertex v = graph.addVertex("1234");
		registryDao.setAuditInfo(v, true);
		assertTrue(v.property(registryContext +"createdBy").isPresent());
		assertTrue(v.property(registryContext +"lastUpdatedBy").isPresent());
		assertEquals(v.property(registryContext +"createdAt").value(),
				v.property(registryContext +"lastUpdatedAt").value());
	}
	
	@Test
	public void test_setAuthInfo_for_update(){
		Graph graph = TinkerGraph.open();
		Vertex v = graph.addVertex("1234");
		registryDao.setAuditInfo(v, true);
		assertTrue(v.property(registryContext +"createdBy").isPresent());
		assertTrue(v.property(registryContext +"lastUpdatedBy").isPresent());
		assertEquals(v.property(registryContext +"createdBy").value().toString(), "sub");
		assertEquals(v.property(registryContext +"lastUpdatedBy").value().toString(), "sub");
		assertThat(v.property(registryContext +"createdAt").value(), instanceOf(Long.class));
		assertThat(v.property(registryContext +"lastUpdatedAt").value(), instanceOf(Long.class));
		assertEquals(v.property(registryContext +"createdAt").value(),
				v.property(registryContext +"lastUpdatedAt").value());
		registryDao.setAuditInfo(v, false);
		assertTrue(v.property(registryContext +"lastUpdatedBy").isPresent());
		assertEquals(v.property(registryContext +"lastUpdatedBy").value().toString(), "sub");
		assertThat(v.property(registryContext +"createdAt").value(), instanceOf(Long.class));
		assertThat(v.property(registryContext +"lastUpdatedAt").value(), instanceOf(Long.class));
	}

	@Test
	public void test_for_delete_root_node() throws DuplicateRecordException, NoSuchElementException, EncryptionException, AuditFailedException, RecordNotFoundException{
		expectedEx.expect(UnsupportedOperationException.class);
		expectedEx.expectMessage(Constants.READ_ON_DELETE_ENTITY_NOT_SUPPORTED);
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel), null, null);
		registryDao.deleteEntityById(response);
		registryDao.getEntityById(response);
		//assertTrue(registryDao.deleteEntityById(response));
	}

	@Test
	public void test_for_delete_child_node_with_root_status_active() throws DuplicateRecordException, NoSuchElementException, EncryptionException, AuditFailedException, RecordNotFoundException{
		/*expectedEx.expect(UnsupportedOperationException.class);
		expectedEx.expectMessage(Constants.DELETE_UNSUPPORTED_OPERATION_ON_ENTITY);*/
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel), null, null);
		assertFalse(registryDao.deleteEntityById("http://example.com/voc/teacher/1.0.0/AreaTypeCode-URBAN"));
		//registryDao.getEntityById(response);
		//assertTrue(registryDao.deleteEntityById(response));
	}

	@Test
	public void test_for_delete_root_node_child_node() throws DuplicateRecordException, NoSuchElementException, EncryptionException, AuditFailedException, RecordNotFoundException{
		/*expectedEx.expect(UnsupportedOperationException.class);
		expectedEx.expectMessage(Constants.DELETE_UNSUPPORTED_OPERATION_ON_ENTITY);*/
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel), null, null);
		registryDao.deleteEntityById(response);
		assertTrue(registryDao.deleteEntityById("http://example.com/voc/teacher/1.0.0/AreaTypeCode-URBAN"));
		//registryDao.getEntityById(response);
		//assertTrue(registryDao.deleteEntityById(response));
	}

	@Test
	public void test_for_delete_when_entity_not_exists() throws RecordNotFoundException, NoSuchElementException, AuditFailedException {

		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);

		UUID label = getLabel();
		registryDao.deleteEntityById(label.toString());
	}


	
}
