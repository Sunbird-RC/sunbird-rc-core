package io.opensaber.registry.dao.impl;

import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.tests.utility.TestHelper;

import io.opensaber.registry.util.RDFUtil;
import org.apache.jena.rdf.model.*;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
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
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.utils.converters.RDF2Graph;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RegistryDaoImpl.class, Environment.class, ObjectMapper.class, GenericConfiguration.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryDaoImplTest extends RegistryTestBase {

	@Autowired
	private RegistryDao registryDao;
	
	@Autowired
	private Environment environment;
	
	@Autowired
	private DatabaseProvider databaseProvider;
	
	@Mock
	private DatabaseProvider mockDatabaseProvider;

	private static Graph graph;

	private static String identifier;
	
	
	private static final String VALID_JSONLD = "school.jsonld";
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
	public void test_adding_a_single_node() throws DuplicateRecordException{
		String label = generateRandomId();
		identifier = label;
		getVertexForSubject(label, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		boolean response = registryDao.addEntity(graph,label);
		assertTrue(response);

	}
	
	
	@Test
	public void test_adding_blank_node() throws NullPointerException, DuplicateRecordException {
		
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);
	}
	
	@Test @Ignore
	public void test_adding_existing_root_node() throws NullPointerException, DuplicateRecordException{
		getVertexForSubject(identifier, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		expectedEx.expect(DuplicateRecordException.class);
		expectedEx.expectMessage(Constants.DUPLICATE_RECORD_MESSAGE);
		registryDao.addEntity(graph,identifier);

	}
	
	@Test
	public void test_adding_multiple_nodes() throws NullPointerException, DuplicateRecordException{
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);

	}
	
	@Test
	public void test_adding_shared_nodes() throws NullPointerException, DuplicateRecordException{
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);
	}
	
	@Test
	public void test_adding_shared_nodes_with_new_properties() throws DuplicateRecordException{
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		Resource resource = ResourceFactory.createResource(rootLabel);
		Property predicate = ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/studentCount");
		Literal literal = ResourceFactory.createPlainLiteral("2000");
		rdfModel.add(resource, predicate, literal.toString());
		updateGraphFromRdf(rdfModel);
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);
	}
	
	@Test
	public void test_adding_shared_nodes_with_updated_properties() throws DuplicateRecordException{
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		Resource resource = ResourceFactory.createResource(rootLabel);
		Property predicate = ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/studentCount");
		Literal literal = ResourceFactory.createPlainLiteral("3000");
		rdfModel.add(resource, predicate, literal.toString());
		updateGraphFromRdf(rdfModel);
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);
	}
	
	public void closeDB() throws Exception {
		databaseProvider.shutdown();
	}
	
	@Test
	public void test_read_with_no_data() throws RecordNotFoundException{
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
	public void test_read_with_some_other_data() throws IOException, DuplicateRecordException, RecordNotFoundException,Exception{
		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);
		String label1 = UUID.randomUUID().toString();
		String label2 = UUID.randomUUID().toString();
		getVertexForSubject(label1, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		registryDao.addEntity(graph,label1);
		registryDao.getEntityById(label2.toString());
		closeDB();
	}

	private void dump_graph(Graph g,String filename) throws IOException {
		g.io(IoCore.graphson()).writeGraph(filename);
	}

	@Test
	public void test_read_single_node() throws RecordNotFoundException, DuplicateRecordException, IOException{
		String label = getLabel().toString();
		getVertexForSubject(label, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		registryDao.addEntity(graph,label);
		Graph entity = registryDao.getEntityById(label);
		assertNotNull(entity);
//		TODO Write a better checker
		assertEquals(countGraphVertices(graph),countGraphVertices(entity));
	}

	@Test
	public void test_read_nested_node() throws NullPointerException, DuplicateRecordException, RecordNotFoundException{
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);
		Graph entity = registryDao.getEntityById(rootLabel);
		assertNotNull(entity);
		try {
			dump_graph(graph, "in.json");
			dump_graph(entity, "out.json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(countGraphVertices(graph),countGraphVertices(entity));
	}
	
	@Test
	public void test_count_nested_node_with_first_node_as_blank_node() throws NullPointerException, DuplicateRecordException, RecordNotFoundException{
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdfWithFirstNodeAsBlankNode(rdfModel);
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);
		Graph entity = registryDao.getEntityById(rootLabel);
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
	public void test_blank_node_count() {
		Model rdfModel = getNewValidRdf();
		java.util.List<RDFNode> blankNodes = RDFUtil.getBlankNodes(rdfModel);
		StmtIterator iterator = rdfModel.listStatements();
		int count = 0;
		while (iterator.hasNext()) {
			Statement stmt = iterator.next();
			if (stmt.getObject().isURIResource()) {
				String uri = stmt.getObject().asResource().getURI();
				count += blankNodes.stream()
						.filter(p -> p.asResource().getURI().equals(uri)).count();
			}
		}
		assertEquals(1, count);
	}

	@Test
	public void test_blank_node_count_when_no_blank_node_present() {
		Model rdfModel = getNewValidRdf();
		removeStattementFromModel(rdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"));

		java.util.List<RDFNode> blankNodes = RDFUtil.getBlankNodes(rdfModel);
		assertEquals(0, blankNodes.size());
	}

	@Test
	public void test_match_blank_nodes() {
		Model rdfModel = getNewValidRdf();
		Resource expectedBlankNode =
				ResourceFactory.createResource("http://example.com/voc/teacher/1.0.0/IndianUrbanPostalAddress");
		List<RDFNode> blankNodes = RDFUtil.getBlankNodes(rdfModel);
		int count = blankNodes.stream()
				.filter(blankNode -> blankNode.equals(expectedBlankNode)).collect(Collectors.toList()).size();
		assertEquals(1, count);
	}

	@Test
	public void test_blank_node_uuid_update() {
		Model rdfModel = getNewValidRdf();
		RDFUtil.updateIdForBlankNode(rdfModel);
		java.util.List<RDFNode> blankNodes = RDFUtil.getBlankNodes(rdfModel);
		assertEquals(0, blankNodes.size());
	}

	@Test
	public void test_update_when_entity_not_exists() throws DuplicateRecordException, RecordNotFoundException {
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		registryDao.addEntity(graph, rootLabel);

		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);

		Graph testGraph = TinkerGraph.open();
		UUID label = getLabel();
		testGraph.addVertex(T.label, label.toString(), "test_label_predicate", "test_value");
		registryDao.updateEntity(testGraph, label.toString());
	}

	@Test
	public void test_update_single_literal_node() throws DuplicateRecordException, RecordNotFoundException, IOException {

		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		registryDao.addEntity(graph, rootLabel);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", rootLabel);
		removeStattementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"));
		removeStattementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"));
		Graph updateGraph = TinkerGraph.open();

		String updateRootNodeLabel = createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, rootLabel);
		assertEquals(rootLabel, updateRootNodeLabel);

		Graph updatedGraphResult = registryDao.getEntityById(rootLabel);

		StringBuilder result = new StringBuilder();
		updatedGraphResult.traversal().V()
				.properties("http://example.com/voc/teacher/1.0.0/revenueBlock")
				.forEachRemaining(p -> result.append(p.value()));
		assertEquals("updated block", result.toString());
	}


	@Test
	public void test_update_multiple_literal_nodes() throws DuplicateRecordException, RecordNotFoundException, IOException {

		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		registryDao.addEntity(graph, rootLabel);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", rootLabel);
		removeStattementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/address"));
		Graph updateGraph = TinkerGraph.open();

		String updateRootNodeLabel = createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, rootLabel);
		assertEquals(rootLabel, updateRootNodeLabel);

		Graph updatedGraphResult = registryDao.getEntityById(rootLabel);

		ArrayList<String> result = new ArrayList<>();
		String prefix = "http://example.com/voc/teacher/1.0.0";

		updatedGraphResult.traversal().V()
				.properties(String.format("%s/revenueBlock", prefix),
						String.format("%s/clusterResourceCentre", prefix))
				.forEachRemaining(p -> result.add(p.value().toString()));
		List<String> expected = Arrays.asList("updated block", "Updated Cluster Resource");
		assertThat(result, is(expected));
		assertThat(result, contains("updated block", "Updated Cluster Resource"));
		assertThat(result, hasSize(2));
	}

	@Test
	public void test_update_iri_node() throws DuplicateRecordException, RecordNotFoundException {

		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		registryDao.addEntity(graph, rootLabel);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", rootLabel);
		removeStattementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/clusterResourceCentre"));
		removeStattementFromModel(updateRdfModel, ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/revenueBlock"));
		Graph updateGraph = TinkerGraph.open();

		String updateRootNodeLabel = createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, rootLabel);
		assertEquals(rootLabel, updateRootNodeLabel);

		Graph updatedGraphResult = registryDao.getEntityById(rootLabel);

		ArrayList<String> result = new ArrayList<>();
		String prefix = "http://example.com/voc/teacher/1.0.0";

		updatedGraphResult.traversal().V()
				.properties(String.format("%s/mohalla", prefix),
						String.format("%s/municipality", prefix))
				.forEachRemaining(p -> result.add(p.value().toString()));
		List<String> expected = Arrays.asList("Updated Sector 14", "Updated MCG");
		assertThat(result, is(expected));
		assertThat(result, contains("Updated Sector 14", "Updated MCG"));
		assertThat(result, hasSize(2));
	}

	@Test
	public void test_update_iri_node_and_literal_nodes() throws DuplicateRecordException, RecordNotFoundException, IOException {

		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		registryDao.addEntity(graph, rootLabel);

		Model updateRdfModel = createRdfFromFile("update_node.jsonld", rootLabel);
		Graph updateGraph = TinkerGraph.open();

		String updateRootNodeLabel = createGraphFromRdf(updateGraph, updateRdfModel);
		registryDao.updateEntity(updateGraph, rootLabel);
		assertEquals(rootLabel, updateRootNodeLabel);

		Graph updatedGraphResult = registryDao.getEntityById(rootLabel);

		ArrayList<String> result = new ArrayList<>();
		String prefix = "http://example.com/voc/teacher/1.0.0";

		updatedGraphResult.traversal().V()
				.properties(String.format("%s/revenueBlock", prefix),
						String.format("%s/clusterResourceCentre", prefix),
						String.format("%s/mohalla", prefix),
						String.format("%s/municipality", prefix))
				.forEachRemaining(p -> result.add(p.value().toString()));

		List<String> expected = Arrays.asList("updated block", "Updated Cluster Resource", "Updated Sector 14", "Updated MCG");
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