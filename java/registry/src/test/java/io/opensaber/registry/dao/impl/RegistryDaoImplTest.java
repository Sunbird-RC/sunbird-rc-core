


package io.opensaber.registry.dao.impl;

import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.tests.utility.TestHelper;

import io.opensaber.registry.util.RDFUtil;
import org.apache.jena.rdf.model.*;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RegistryDaoImpl.class, Environment.class, ObjectMapper.class, GenericConfiguration.class,EncryptionServiceImpl.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryDaoImplTest extends RegistryTestBase {

	@Autowired
	private RegistryDao registryDao;
	
	@Autowired
	private Environment environment;
	
	@Autowired
	private DatabaseProvider databaseProvider;
	
	@Autowired
	private EncryptionService encryptionService;
	
	@Mock
	private DatabaseProvider mockDatabaseProvider;

	private static Graph graph;

	private static String identifier;

	private static final String VALID_JSONLD = "school.jsonld";
    private static final String RICH_LITERAL_TTL = "rich-literal.jsonld";
	private static final String CONTEXT_CONSTANT = "sample:";

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

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
	}

	@Test
	public void test_adding_a_single_node() throws DuplicateRecordException, RecordNotFoundException, EncryptionException {
		String label = generateRandomId();
		identifier = label;
		getVertexForSubject(label, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		String response = registryDao.addEntity(graph, label);
		Graph entity = registryDao.getEntityById(response);
		assertEquals(1, IteratorUtils.count(entity.traversal().clone().V()));
		Vertex v = entity.traversal().V().has(T.label, label).next();
		assertEquals("DAV Public School", v.property("http://example.com/voc/teacher/1.0.0/schoolName").value());
	}
	
	@Test @Ignore
	public void test_adding_existing_root_node() throws NullPointerException, DuplicateRecordException, EncryptionException {
		getVertexForSubject(identifier, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		expectedEx.expect(DuplicateRecordException.class);
		expectedEx.expectMessage(Constants.DUPLICATE_RECORD_MESSAGE);
		registryDao.addEntity(graph, identifier);
	}
	
	@Test
	public void test_adding_multiple_nodes() throws NullPointerException, DuplicateRecordException, RecordNotFoundException, EncryptionException {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, String.format("_:%s", rootLabel));
		Graph entity = registryDao.getEntityById(response);
		long vertexCount = IteratorUtils.count(entity.traversal().clone().V());
		assertEquals(5, vertexCount);
	}

	@Test
	public void test_adding_shared_nodes() {
		try {
			Model rdfModel1 = getNewValidRdf();
			TinkerGraph graphEntity1 = TinkerGraph.open();
			String rootLabelEntity1 = createGraphFromRdf(graphEntity1, rdfModel1);
			String entity1Label = registryDao.addEntity(graphEntity1, "_:" + rootLabelEntity1);

			Graph entity1 = registryDao.getEntityById(entity1Label);

			// Expected count of vertices in one entity
			assertEquals(5, IteratorUtils.count(entity1.traversal().V()));

			Model rdfModel2 = getNewValidRdf();
			TinkerGraph graphEntity2 = TinkerGraph.open();
			String rootLabelEntity2 = createGraphFromRdf(graphEntity2, rdfModel2);
			String entity2Label = registryDao.addEntity(graphEntity2, "_:" + rootLabelEntity2);

			Graph entity2 = registryDao.getEntityById(entity2Label);

			assertEquals(5, IteratorUtils.count(entity2.traversal().V()));

			long verticesCountAfterSharedNodesCreation = IteratorUtils.count(databaseProvider.getGraphStore().traversal().clone().V());
			long edgesCountAfterSharedNodesCreation = IteratorUtils.count(databaseProvider.getGraphStore().traversal().clone().E());

			// Expected count of vertices is 6 with two entities with same address created
			assertEquals(7, verticesCountAfterSharedNodesCreation);
			assertEquals(8, edgesCountAfterSharedNodesCreation);

		} catch (DuplicateRecordException | RecordNotFoundException | EncryptionException | NoSuchElementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void test_adding_shared_nodes_with_new_properties() throws DuplicateRecordException, RecordNotFoundException, EncryptionException {

		// Add a new entity
		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		registryDao.addEntity(graph, String.format("_:%s", rootLabel));

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

		String newEntityResponse = registryDao.addEntity(newEntityGraph, String.format("_:%s", newRootLabel));
		Graph entity = registryDao.getEntityById(newEntityResponse);
		String propertyValue = (String) entity.traversal().clone().V()
			.properties("http://example.com/voc/teacher/1.0.0/districtAlias")
			.next()
			.value();
		assertEquals("Gurgaon alias", propertyValue);

	}
	
	@Test
	public void test_adding_shared_nodes_with_updated_properties() throws DuplicateRecordException, RecordNotFoundException, EncryptionException {

		// Add a new entity
		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		registryDao.addEntity(graph, String.format("_:%s", rootLabel));

		// Create a new TinkerGraph with the existing jsonld
		Graph newEntityGraph = TinkerGraph.open();
		Model newRdfModel = getNewValidRdf();
		updateNodeLabel(newRdfModel, "http://example.com/voc/teacher/1.0.0/IndianUrbanPostalAddress");
		removeStattementFromModel(newRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/municipality"));

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
	public void test_read_with_no_data() throws RecordNotFoundException, EncryptionException{
		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);
		UUID label = getLabel();
		Graph graph = registryDao.getEntityById(label.toString());
	}

	private UUID getLabel() {
		UUID label = UUID.randomUUID();
		return label;
	}
	
	
	@Test
	public void test_read_with_some_other_data()
			throws IOException, DuplicateRecordException, RecordNotFoundException, EncryptionException, Exception {

		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);
		String label1 = UUID.randomUUID().toString();
		String label2 = UUID.randomUUID().toString();
		getVertexForSubject(label1, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		String response = registryDao.addEntity(graph, label1);
		registryDao.getEntityById(label2);
		closeDB();
	}

	private void dump_graph(Graph g,String filename) throws IOException {
		g.io(IoCore.graphson()).writeGraph(filename);
	}

	@Test
	public void test_read_single_node()
			throws RecordNotFoundException, DuplicateRecordException, IOException, EncryptionException {

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
			throws NullPointerException, DuplicateRecordException, RecordNotFoundException, EncryptionException {

		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);
		Graph entity = registryDao.getEntityById(response);
		assertNotNull(entity);
		try {
			dump_graph(graph, "in.json");
			dump_graph(entity, "out.json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(countGraphVertices(graph), countGraphVertices(entity));
	}
	
	@Test
	public void test_count_nested_node_with_first_node_as_blank_node()
			throws NullPointerException, DuplicateRecordException, RecordNotFoundException, NoSuchElementException, EncryptionException {

		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdfWithFirstNodeAsBlankNode(rdfModel);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);
		Graph entity = registryDao.getEntityById(response);
		assertNotNull(entity);
		try {
			dump_graph(graph, "in_count.json");
			dump_graph(entity, "out_count.json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(countGraphVertices(graph),countGraphVertices(entity));
	}

	@Test
	public void test_blank_node_count_when_no_blank_node_present()
			throws DuplicateRecordException, RecordNotFoundException, EncryptionException {

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
			throws DuplicateRecordException, RecordNotFoundException, EncryptionException, NoSuchElementException {

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
			throws DuplicateRecordException, RecordNotFoundException, EncryptionException, NoSuchElementException {

		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", response);
		removeStattementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"));
		removeStattementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"));
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/School");
		Graph updateGraph = TinkerGraph.open();

		createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, response, "addOrUpdate");

		Graph updatedGraphResult = registryDao.getEntityById(response);

		StringBuilder result = new StringBuilder();
		updatedGraphResult.traversal().V()
				.properties("http://example.com/voc/teacher/1.0.0/revenueBlock")
				.forEachRemaining(p -> result.append(p.value()));
		assertEquals("updated block", result.toString());
	}

	@Test
	public void test_update_multiple_literal_nodes()
			throws DuplicateRecordException, RecordNotFoundException, EncryptionException, NoSuchElementException {

		Model rdfModel = getNewValidRdf();
		TinkerGraph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", response);
		removeStattementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"));
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/School");
		Graph updateGraph = TinkerGraph.open();

		createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, response, "addOrUpdate");

		Graph updatedGraphResult = registryDao.getEntityById(response);

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
			throws DuplicateRecordException, RecordNotFoundException, NoSuchElementException, EncryptionException {

		Model rdfModel = getNewValidRdf();
		Graph graph = TinkerGraph.open();
		String rootLabel = updateGraphFromRdf(rdfModel, graph);
		String response = registryDao.addEntity(graph, "_:"+rootLabel);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", rootLabel);
		// Remove literal properties from update
		removeStattementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"));
		removeStattementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/revenueBlock"));
		// Update new rdf model with the labels generated for School and Address nodes
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/School");
		updateNodeLabel(updateRdfModel, "http://example.com/voc/teacher/1.0.0/IndianUrbanPostalAddress");

		// Call update entity
		Graph updateGraph = TinkerGraph.open();
		createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, response, "addOrUpdate");
		Graph updatedGraphResult = registryDao.getEntityById(response);

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
			throws DuplicateRecordException, RecordNotFoundException, EncryptionException, NoSuchElementException {

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
		
	/*
		@Test
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
	public void closeGraph() throws Exception{
		if(graph!=null){
			graph.close();
		}
	}

	@Test
	public void savingMetaProperties() throws DuplicateRecordException, RecordNotFoundException, EncryptionException {
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

	private void removeStattementFromModel(Model rdfModel, Property predicate) {
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

	private Vertex getVertexForSubject(String subjectValue, String property, String objectValue){
		Vertex vertex = null;
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

	
	private Model getNewValidRdf(){
		return getNewValidRdf(VALID_JSONLD, CONTEXT_CONSTANT);
		
	}

	private Model createRdfFromFile(String jsonldFilename, String rootNodeLabel) {
		return getNewValidRdf(jsonldFilename, CONTEXT_CONSTANT, rootNodeLabel);
	}

    private String updateGraphFromRdf(Model rdfModel) {
        return updateGraphFromRdf(rdfModel, graph, environment.getProperty(Constants.SUBJECT_LABEL_TYPE));
    }

    private String updateGraphFromRdf(Model rdfModel, Graph graph) {
		return updateGraphFromRdf(rdfModel, graph, environment.getProperty(Constants.SUBJECT_LABEL_TYPE));
	}

	private String updateGraphFromRdf(Model rdfModel, Graph graph, String rootLabelType) {
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
	
}
