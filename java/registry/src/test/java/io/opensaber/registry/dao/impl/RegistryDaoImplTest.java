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
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
/*import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;*/

import io.opensaber.registry.controller.RegistryTestBase;
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
public class RegistryDaoImplTest extends RegistryTestBase{

	@Autowired
	RegistryDao registryDao;
	
	@Autowired
	private Environment environment;

	private static Graph graph;

	private static String identifier;
	private final String type = environment.getProperty(Constants.SUBJECT_LABEL_TYPE);
	
	private static final String VALID_JSONLD1 = "school1.jsonld";
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	private static final String SUBJECT_LABEL = "ex:Picasso";

	@Before
	public void initializeGraph(){
		graph = TinkerGraph.open();
		
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
		/*Model rdfModel = getNewValidRdf(VALID_JSONLD1, type);
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
		}*/
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		/*ValueFactory vf = SimpleValueFactory.getInstance();
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
		editGraph(graph, modelBuilder.build());*/
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);
	}
	
	@Test
	public void test_add_existing_root_node() throws NullPointerException, DuplicateRecordException{
		getVertexForSubject(identifier, "http://example.com/voc/teacher/1.0.0/schoolName", "DAV Public School");
		expectedEx.expect(DuplicateRecordException.class);
		expectedEx.expectMessage(Constants.DUPLICATE_RECORD_MESSAGE);
		registryDao.addEntity(graph,identifier);

	}
	
	@Test
	public void test_add_with_multiple_nodes() throws NullPointerException, DuplicateRecordException{
		/*identifier = generateRandomId();
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
		firstVertex.addEdge("lives in", thirdVertex);*/
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);

	}
	
	@Test
	public void test_add_with_shared_nodes() throws NullPointerException, DuplicateRecordException{
		/*identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		String firstVertexLabel = "teacher2";
		String secondVertexLabel = "school";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "email", "test@bhavans.com");
		firstVertex.addEdge("works in", secondVertex);*/
		Model rdfModel = getNewValidRdf();
		String rootLabel = updateGraphFromRdf(rdfModel);
		boolean response = registryDao.addEntity(graph,rootLabel);
		assertTrue(response);
	}
	
	@Test
	public void test_add_with_shared_nodes_add_new_properties() throws DuplicateRecordException{
		/*identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		String firstVertexLabel = "teacher3";
		String secondVertexLabel = "school";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "studentCount", "1500");
		firstVertex.addEdge("works in", secondVertex);*/
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
	public void test_add_with_shared_nodes_update_existing_properties() throws DuplicateRecordException{
		/*identifier = generateRandomId();
		Vertex firstVertex = null;
		Vertex secondVertex = null;
		String firstVertexLabel = "teacher4";
		String secondVertexLabel = "school";
		getVertexForSubject(firstVertexLabel, "identifier", identifier);
		firstVertex = getVertexForSubject(firstVertexLabel, "type", "Teacher");
		getVertexForSubject(secondVertexLabel, "name", "bhavans");
		secondVertex = getVertexForSubject(secondVertexLabel, "email", "test@bvb.com");
		firstVertex.addEdge("works in", secondVertex);*/
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


	/*private void editGraph(Graph graph, Model simpleRDFModel) {
		clearGraph(graph);
		for(Statement rdfStatement: simpleRDFModel) {
			RDF2Graph.convertRDFStatement2Graph(rdfStatement, graph);
		}
	}*/

	private void clearGraph(Graph graph) {
		graphTraversal(graph).drop().iterate();
	}

	private GraphTraversal<Vertex, Vertex> graphTraversal(Graph graph) {
		return graph.traversal().V();
	}


	
	/*private ModelBuilder createSimpleRDF(String subjectLabel){
		ModelBuilder builder = new ModelBuilder();
		return builder
				.setNamespace("ex", "http://example.org/")
				.subject(subjectLabel)
				.add(FOAF.FIRST_NAME, "Pablo");
	}*/
	
	private Model getNewValidRdf(){
		return getNewValidRdf(VALID_JSONLD1, type);
		
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
	
}
