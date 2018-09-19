package io.opensaber.utils.converters;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.hamcrest.core.Every;
import org.junit.*;

import java.io.*;
import java.util.Set;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

public class RDF2GraphTest {

	private static final String SUBJECT_LABEL = "ex:Picasso";
	private static final String SUBJECT_EXPANDED_LABEL = "http://example.org/Picasso";
	private static final String CONTEXT = "http://example.org/";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void createGraphFromTriple() throws Exception {
		Graph graph = createGraph();
		ModelBuilder modelBuilder = createSimpleRDF(SUBJECT_LABEL);
		assertEquals(0,countGraphVertices(graph));
		editGraph(graph, modelBuilder.build());
//		Test for single vertex 
		assertEquals(1,countGraphVertices(graph));
//		test for that vertex having the property expected
		GraphTraversal<Vertex, Vertex> hasP = 
				graphTraversal(graph).has(FOAF.FIRST_NAME.toString(),"Pablo");
		assertTrue(hasP.hasNext());
		hasP.next();
		assertFalse(hasP.hasNext());
		modelBuilder = modelBuilder
				.add(FOAF.BIRTHDAY, "1987-08-25");
		editGraph(graph, modelBuilder.build());
//		test that a vertex did not get added since the object is only a Literal
		assertEquals(1,countGraphVertices(graph));
		modelBuilder = modelBuilder
				.add(RDF.TYPE, "ex:Artist");
		editGraph(graph, modelBuilder.build());
//		test that a vertex got added since the object is an IRI
		assertEquals(2,countGraphVertices(graph));
		modelBuilder = modelBuilder
				.add(FOAF.DEPICTION, "ex:Image");
		editGraph(graph, modelBuilder.build());
		assertEquals(3,countGraphVertices(graph));
	}

	@Test
	public void handleDupeInTriple() throws Exception {
		Graph graph = createGraph();
		ModelBuilder modelBuilder = createSimpleRDF(SUBJECT_LABEL)
				.add(FOAF.BIRTHDAY, "1987-08-25")
				.add(FOAF.BIRTHDAY, "1987-08-25");
		editGraph(graph, modelBuilder.build());
		assertEquals(1,countGraphVertices(graph));
	}

	@Test
	public void handleBNodeInTriple() throws Exception {
		Graph graph = createGraph();
		ValueFactory vf = SimpleValueFactory.getInstance();
		BNode address = vf.createBNode();
		BNode painting = vf.createBNode();
		BNode reaction = vf.createBNode();
		ModelBuilder modelBuilder = createSimpleRDF(SUBJECT_LABEL);
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
		dumpGraph(graph);
//		1 subject, 3 Blank Nodes and 3 object IRIs
		assertEquals(7,countGraphVertices(graph));
		GraphTraversal<Vertex, Vertex> hasP = 
				graphTraversal(graph)
					.has(T.label,"http://example.org/Picasso")
					.out("http://example.org/creatorOf")
					.out("http://example.org/reaction");
		assertTrue(hasP.hasNext());
	}

	@Test
	public void handleRecursiveBNodeInTriple() throws Exception {
	}
	
	@Test
	public void createTripleFromGraphWithOnlySingleVertex(){
		Graph graph = createGraph();
		ModelBuilder modelBuilder = createSimpleRDF(SUBJECT_LABEL);
		assertEquals(0,countGraphVertices(graph));
		editGraph(graph, modelBuilder.build());
		Model rdfModel = RDF2Graph.convertGraph2RDFModel(graph, SUBJECT_EXPANDED_LABEL);
		System.out.println(rdfModel);
		assertEquals(1, rdfModel.size()); 
		String[] expected = {
		                     "http://example.org/Picasso",
		                     "http://xmlns.com/foaf/0.1/firstName",
		                     "\"Pablo\"^^<http://www.w3.org/2001/XMLSchema#string>"
		};
		for(Statement rdfStatement: rdfModel) {
			Value subjectValue = rdfStatement.getSubject();
			IRI property = rdfStatement.getPredicate();
			Value objectValue = rdfStatement.getObject();
			assertEquals(expected[0], subjectValue.toString());
			assertEquals(expected[1], property.toString());
			assertEquals(expected[2], objectValue.toString());
		}
	}
	
	@Test
	public void createTripleFromTwoVertices() throws IOException{
		Graph graph = createGraph();
		ModelBuilder modelBuilder = createSimpleRDF(SUBJECT_LABEL);
		modelBuilder = modelBuilder
				.add(RDF.TYPE, "ex:Artist")
				.add(FOAF.DEPICTION, "ex:Image");
		editGraph(graph, modelBuilder.build());
		assertEquals(3,countGraphVertices(graph));
		dumpGraph(graph);
		Model rdfModel = RDF2Graph.convertGraph2RDFModel(graph, SUBJECT_EXPANDED_LABEL);
		assertEquals(3, rdfModel.size());
		String[] expected = 
		{
			"http://example.org/Picasso",
			"http://xmlns.com/foaf/0.1/firstName",
			"\"Pablo\"^^<http://www.w3.org/2001/XMLSchema#string>",
			"http://example.org/Picasso",
			"http://xmlns.com/foaf/0.1/depiction",
			"http://example.org/Image",
			"http://example.org/Picasso",
			"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
			"http://example.org/Artist"
		};
		int i = 0;
		for(Statement rdfStatement: rdfModel) {
			Value subjectValue = rdfStatement.getSubject();
			IRI property = rdfStatement.getPredicate();
			Value objectValue = rdfStatement.getObject();
			System.out.println(rdfStatement);
			assertEquals(expected[i], subjectValue.toString());
			assertEquals(expected[i+1], property.toString());
			assertEquals(expected[i+2], objectValue.toString());
			i+=3;
		}
	}
	
	@Test
	public void createTriplesFromBNode(){
		Graph graph = createGraph();
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI address = vf.createIRI("ex:specific_address_id");
		BNode painting = vf.createBNode();
		BNode reaction = vf.createBNode();
		ModelBuilder modelBuilder = createSimpleRDF(SUBJECT_LABEL);
		modelBuilder
			.add(RDF.TYPE, "ex:Artist")
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
		Model expectedModel = modelBuilder.build();
		editGraph(graph, expectedModel);
		Model convertedModel = RDF2Graph.convertGraph2RDFModel(graph, SUBJECT_EXPANDED_LABEL);
		IRI blankPred = vf.createIRI("http://example.org/creatorOf");
		checkNodes(graph, expectedModel, blankPred, convertedModel, BNode.class);
		IRI iriPred = vf.createIRI("http://example.org/homeAddress");
		checkNodes(graph, expectedModel, iriPred, convertedModel, IRI.class);
		assertEquals(expectedModel.size(), convertedModel.size()); 
		System.out.println(expectedModel);
		System.out.println(convertedModel);
	}

	@Test
	public void testDataTypePersistedInGraph() throws IOException {
        Model model = Rio.parse(getInputStream("rich-literal.ttl"), "", RDFFormat.TURTLE);
		Graph graph = createGraph();
        editGraph(graph, model);
        Model _model = RDF2Graph.convertGraph2RDFModel(graph,"http://example.org/someSubject");
        Rio.write(model, System.out, RDFFormat.TURTLE);
        Rio.write(_model, System.out, RDFFormat.TURTLE);
        Assert.assertTrue(Models.isomorphic(model,_model));
	}

	private void checkNodes(Graph graph, Model rdfModel, IRI iriPred, Model expectedModel, Class _class) {
		Set<Value> actualObjects = rdfModel.filter(null, iriPred, null).objects();
		Set<Value> expectedObjects = expectedModel.filter(null, iriPred, null).objects();
		Assert.assertThat(expectedObjects, (Every.everyItem(instanceOf(_class))));
		Assert.assertThat(actualObjects, (Every.everyItem(instanceOf(_class))));
	}

	private long countGraphVertices(Graph graph) {
		return IteratorUtils.count(graph.vertices());
	}

	private void editGraph(Graph graph, Model simpleRDFModel) {
		clearGraph(graph);
		for(Statement rdfStatement: simpleRDFModel) {
			RDF2Graph.convertRDFStatement2Graph(rdfStatement, graph, CONTEXT);
		}
	}

	private void clearGraph(Graph graph) {
		graphTraversal(graph).drop().iterate();
	}

	private GraphTraversal<Vertex, Vertex> graphTraversal(Graph graph) {
		return graph.traversal().V();
	}

	private void dumpGraph(Graph graph) throws IOException {
		graph.io(IoCore.graphson()).writeGraph("dump.json");
	}
	
	private ModelBuilder createSimpleRDF(String subjectLabel){
		ModelBuilder builder = new ModelBuilder();
		return builder
				.setNamespace("ex", "http://example.org/")
				.subject(subjectLabel)
				.add(FOAF.FIRST_NAME, "Pablo");
	}
	
	private Graph createGraph(){
		Graph graph = TinkerGraph.open();
		return graph;
	}

	private InputStream getInputStream(String filename) throws FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filename).getFile());
        return new FileInputStream(file);
    }
}
