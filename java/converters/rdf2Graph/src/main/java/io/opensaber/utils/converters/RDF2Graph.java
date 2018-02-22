package io.opensaber.utils.converters;

import java.util.Iterator;
import java.util.Stack;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;


public final class RDF2Graph 
{
	private RDF2Graph() {}
	
	public static String getRootSubjectLabel(org.apache.jena.rdf.model.Statement rdfStatement, String type){
		String subjectValue = rdfStatement.getSubject().toString();
		String predicate = rdfStatement.getPredicate().toString();
		String label = null;
		if(predicate.equals(RDF.TYPE.toString())){
			RDFNode object = rdfStatement.getObject();
			if(object.isURIResource()){
				if(object.toString().equals(type)){
					label = subjectValue;
					System.out.println("Printing label:"+label);
				}
			}
		}
		return label;
	}

	public static Graph convertJenaRDFStatement2Graph(org.apache.jena.rdf.model.Statement statement, Graph graph){
		ValueFactory factory = SimpleValueFactory.getInstance();
		org.apache.jena.rdf.model.Resource subject = statement.getSubject();
		RDFNode object =statement.getObject();
		Property property = statement.getPredicate();
		Value subjectValue = getValueFromObject(subject, factory);
		Value objectValue = getValueFromObject(object, factory);
		IRI predicate = factory.createIRI(property.toString());
		Statement finalStatement = factory.createStatement((Resource)subjectValue, predicate, objectValue);
		graph = convertRDFStatement2Graph(finalStatement, graph);
		return graph;
	}

	private static Value getValueFromObject(org.apache.jena.rdf.model.Resource object, ValueFactory factory){
		Value value = null;
		if(object.isLiteral()){
			value = factory.createLiteral(object.toString());
		}else if(object.isURIResource()){
			value = factory.createIRI(object.toString());
		}else{
			value = factory.createBNode(object.toString());
		}

		return value;

	}

	private static Value getValueFromObject(RDFNode object, ValueFactory factory){
		Value value = null;
		if(object.isLiteral()){
			value = factory.createLiteral(object.toString());
		}else if(object.isURIResource()){
			value = factory.createIRI(object.toString());
		}else{
			value = factory.createBNode(object.toString());
		}

		return value;

	}

	public static Graph convertRDFStatement2Graph(Statement rdfStatement, Graph graph) {
		Value subjectValue = rdfStatement.getSubject();
		IRI property = rdfStatement.getPredicate();
		Value objectValue = rdfStatement.getObject();
		updateGraph(subjectValue, property, objectValue, graph);
		return graph;
	}

	private static void updateGraph(Value subjectValue, IRI property, Value objectValue, Graph graph) {
		GraphTraversalSource t = graph.traversal();
		GraphTraversal<Vertex, Vertex> hasLabel = t.V().hasLabel(subjectValue.toString());
		Vertex s;
		if(hasLabel.hasNext()){
			s = hasLabel.next();
		} else {
			s = graph.addVertex(
					T.label,
					subjectValue.toString());
		}
		if (objectValue instanceof Literal) {
			Literal literal = (Literal)objectValue;
			s.property(property.toString(), literal.getLabel());

		} else if (objectValue instanceof IRI) {
			IRI objectIRI = (IRI)objectValue;
			Vertex o = graph.addVertex(
					T.label,
					objectIRI.toString());
			s.addEdge(property.toString(), o);
		} else if (objectValue instanceof BNode) {
			BNode objectBNode = (BNode)objectValue;
			Vertex o = graph.addVertex(
					T.label,
					objectBNode.toString());
			s.addEdge(property.toString(), o);
		}
	}

	public static Model convertGraph2RDFModel(Graph graph, String label) {
		ModelBuilder builder = new ModelBuilder();
		GraphTraversalSource t = graph.traversal();
		GraphTraversal<Vertex, Vertex> hasLabel = t.V().hasLabel(label);
		Vertex s;
		if(hasLabel.hasNext()){
			s = hasLabel.next();
			extractModelFromVertex(builder, s);
		}
		return builder.build();
	}

	private static void extractModelFromVertex(ModelBuilder builder, Vertex s) {
		builder.subject(s.label());
		Iterator<VertexProperty<String>> propertyIter = s.properties();
		while (propertyIter.hasNext()){
			VertexProperty<String> property = propertyIter.next();
			builder.add(property.label(), property.value());
		}
		Iterator<Edge> edgeIter = s.edges(Direction.OUT);
		Edge edge;
		Stack<Vertex> vStack = new Stack<Vertex>();
		while(edgeIter.hasNext()){
			edge = edgeIter.next();
			s = edge.inVertex();
			builder.add(edge.label(), s.label());
			vStack.push(s);
		}
		Iterator<Vertex> vIterator = vStack.iterator();
		while(vIterator.hasNext()){
			s = vIterator.next();
			extractModelFromVertex(builder,s);
		}
	}
}
