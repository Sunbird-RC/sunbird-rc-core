package io.opensaber.registry.dao.impl;

import io.opensaber.registry.sink.DatabaseProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import cucumber.api.java.en.When;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.http4s.CacheDirective;

import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.utils.converters.RDF2Graph;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.UUID;
import static org.mockito.Mockito.*;


@RunWith(SpringRunner.class)
@SpringBootTest(classes={RegistryDaoImpl.class
		,Environment.class,ObjectMapper.class,GenericConfiguration.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryDaoImplTest extends RegistryTestBase{

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
	
	
	private static final String VALID_JSONLD1 = "school1.jsonld";
	private static final String CONTEXT_CONSTANT = "sample:";
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();


	@Before
	public void initializeGraph(){
		graph = TinkerGraph.open();
		MockitoAnnotations.initMocks(this);
		
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
	
	@Test
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
	public void test_adding_shared_nodes_with_updated_properties() throws DuplicateRecordException,Exception{
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		Resource resource = ResourceFactory.createResource(rootLabel);
		Property predicate = ResourceFactory.createProperty("http://example.com/voc/teacher/1.0.0/studentCount");
		Literal literal = ResourceFactory.createPlainLiteral("3000");
		rdfModel.add(resource, predicate, literal.toString());
		updateGraphFromRdf(rdfModel);
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);
		closeDB();
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
	public void test_read_with_some_other_data() throws IOException, DuplicateRecordException, RecordNotFoundException{
		expectedEx.expect(RecordNotFoundException.class);
		expectedEx.expectMessage(Constants.ENTITY_NOT_FOUND);
		String label1 = UUID.randomUUID().toString();
		String label2 = UUID.randomUUID().toString();
		getVertexForSubject(label1, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		registryDao.addEntity(graph,label1);
		registryDao.getEntityById(label2.toString());
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
		Graph entity = registryDao.getEntityById("http://example.com/voc/teacher/1.0.0/1236");
		assertNotNull(entity);
		try {
			dump_graph(graph, "incoming.json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(countGraphVertices(graph),countGraphVertices(entity));
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
		return getNewValidRdf(VALID_JSONLD1, getSubjectType(),CONTEXT_CONSTANT);
		
	}
	
	private String updateGraphFromRdf(Model rdfModel){
		StmtIterator iterator = rdfModel.listStatements();
		boolean rootSubjectFound = false;
		String label = null;
		while(iterator.hasNext()){
			Statement rdfStatement = iterator.nextStatement();
			if(!rootSubjectFound){
				String type = environment.getProperty(Constants.SUBJECT_LABEL_TYPE);
				label = RDF2Graph.getRootSubjectLabel(rdfStatement,type);
				if(label!=null){
					rootSubjectFound = true;
				}
			}
			graph = RDF2Graph.convertJenaRDFStatement2Graph(rdfStatement, graph);
	}
		return label;
	}

	private long countGraphVertices(Graph graph) {
		return IteratorUtils.count(graph.vertices());
	}
	
}