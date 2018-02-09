package io.opensaber.registry.dao.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;


import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.utils.converters.RDF2Graph;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;





@RunWith(SpringRunner.class)
@SpringBootTest(classes={RegistryDaoImpl.class
		,Environment.class,ObjectMapper.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RegistryDaoImplTest {

	@Autowired
	RegistryDao registryDao;

	private static TinkerGraph graph;

	private static String identifier;
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	private static final String SUBJECT_LABEL = "ex:Picasso";
	private static final String SUBJECT_EXPANDED_LABEL = "http://example.org/Picasso";

	@Before
	public void initializeGraph(){
		graph = TinkerGraph.open();
		
	}

	@Test
	public void test_add_entity_with_single_node() throws NullPointerException, DuplicateRecordException{
		identifier = generateRandomId();
		String label = "teacher";
		getVertexForSubject(label, "identifier", identifier);
		getVertexForSubject(label, "type", "Teacher");
		boolean response = registryDao.addEntity(graph,label);
		assertTrue(response);

	}
	
	
	@Test
	public void test_add_with_blank_node() throws NullPointerException, DuplicateRecordException {
		ValueFactory vf = SimpleValueFactory.getInstance();
		BNode address = vf.createBNode();
		BNode painting = vf.createBNode();
		BNode reaction = vf.createBNode();
		ModelBuilder modelBuilder = createSimpleRDF(SUBJECT_LABEL);
		editGraph(graph, modelBuilder.build());
		modelBuilder = modelBuilder
				.add(RDF.TYPE, "ex:Artist");
		editGraph(graph, modelBuilder.build());
		modelBuilder = modelBuilder
				.add(FOAF.DEPICTION, "ex:Image");
		editGraph(graph, modelBuilder.build());
		modelBuilder
			.add("ex:homeAddress", address)
			.add("ex:creatorOf", painting)
			.subject(address)
				.add("ex:street", "31 Art Gallery")
				.add("ex:city", "Madrid")
				.add("ex:country", "Spain")
				.add(RDF.TYPE,"ex:PostalAddress")
			.subject(painting)
				.add(RDF.TYPE,"ex:CreativeWork")
				.add("ex:depicts", "cubes")
				.add("ex:reaction", reaction)
				.subject(reaction)
					.add("ex:rating","5")
					.add(RDF.TYPE,"ex:AggregateRating");
		editGraph(graph, modelBuilder.build());
		boolean response = registryDao.addEntity(graph,SUBJECT_LABEL);
		assertTrue(response);
	}
	
	@Test
	public void test_add_existing_entity() throws NullPointerException, DuplicateRecordException{
		String label = "teacher";
		getVertexForSubject(label, "identifier", identifier);
		getVertexForSubject(label, "type", "Teacher");
		expectedEx.expect(DuplicateRecordException.class);
		expectedEx.expectMessage(Constants.DUPLICATE_RECORD_MESSAGE);
		registryDao.addEntity(graph,label);

	}
	
	@Test
	public void test_add_with_multiple_nodes() throws NullPointerException, DuplicateRecordException{
		identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		Vertex thirdVertex = null;
		String firstVertexLabel = "teacher1";
		String secondVertexLabel = "school";
		String thirdVertexLabel = "address";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "email", "test@bhavans.com");
		firstVertex.addEdge("works in", secondVertex);
		getVertexForSubject(thirdVertexLabel, "building", "#90");
		thirdVertex = getVertexForSubject(thirdVertexLabel, "street", "1st main");
		firstVertex.addEdge("lives in", thirdVertex);
		boolean response = registryDao.addEntity(graph,firstVertexLabel);
		assertTrue(response);

	}
	
	@Test
	public void test_add_with_shared_nodes() throws NullPointerException, DuplicateRecordException{
		identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		String firstVertexLabel = "teacher2";
		String secondVertexLabel = "school";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "email", "test@bhavans.com");
		firstVertex.addEdge("works in", secondVertex);
		boolean response = registryDao.addEntity(graph,firstVertexLabel);
		assertTrue(response);
	}
	
	@Test
	public void test_add_with_shared_nodes_add_new_properties() throws NullPointerException, DuplicateRecordException{
		identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		String firstVertexLabel = "teacher3";
		String secondVertexLabel = "school";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "studentCount", "1500");
		firstVertex.addEdge("works in", secondVertex);
		boolean response = registryDao.addEntity(graph,firstVertexLabel);
		assertTrue(response);
	}
	
	@Test
	public void test_add_with_shared_nodes_update_existing_properties() throws NullPointerException, DuplicateRecordException{
		identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		String firstVertexLabel = "teacher4";
		String secondVertexLabel = "school";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "email", "test@bvb.com");
		firstVertex.addEdge("works in", secondVertex);
		boolean response = registryDao.addEntity(graph,firstVertexLabel);
		assertTrue(response);
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

	@Test
	public void testModifyNonExistingEntity(){
		//GraphTraversalSource t = graph.traversal();
		//Vertex vertex = null;
		getVertexForSubject("identifier","is", generateRandomId());
		getVertexForSubject("type","is", "teacher");
		getVertexForSubject("email","is", "consent driven");
		boolean response = registryDao.updateEntity(graph);
		assertFalse(response);
	}


	@Test
	public void testRemoveEntity(){
		EntityDto entityDto = new EntityDto();
		entityDto.setId(identifier);
		boolean response = registryDao.deleteEntity(entityDto);
		assertTrue(response);

	}

	@Test
	public void testRemoveNonExistingEntity(){
		EntityDto entityDto = new EntityDto();
		entityDto.setId(identifier);
		boolean response = registryDao.deleteEntity(entityDto);
		assertTrue(response);

	}*/

	@After
	public void closeGraph() throws Exception{
		if(graph!=null){
			graph.close();
		}
	}

	private static String generateRandomId(){
		return UUID.randomUUID().toString();
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
	
	private long countGraphVertices(Graph graph) {
		return IteratorUtils.count(graph.vertices());
	}

	private void editGraph(Graph graph, Model simpleRDFModel) {
		clearGraph(graph);
		for(Statement rdfStatement: simpleRDFModel) {
			RDF2Graph.convertRDFStatement2Graph(rdfStatement, graph);
		}
	}

	private void clearGraph(Graph graph) {
		graphTraversal(graph).drop().iterate();
	}

	private GraphTraversal<Vertex, Vertex> graphTraversal(Graph graph) {
		return graph.traversal().V();
	}


	
	private ModelBuilder createSimpleRDF(String subjectLabel){
		ModelBuilder builder = new ModelBuilder();
		return builder
				.setNamespace("ex", "http://example.org/")
				.subject(subjectLabel)
				.add(FOAF.FIRST_NAME, "Pablo");
	}
	
}
