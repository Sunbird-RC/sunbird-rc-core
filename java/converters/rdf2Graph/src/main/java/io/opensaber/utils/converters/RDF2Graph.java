package io.opensaber.utils.converters;


import java.util.Iterator;
import java.util.Stack;

import org.apache.jena.rdf.model.ModelFactory;
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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class RDF2Graph 
{
	private RDF2Graph() {}

	private static Logger logger = LoggerFactory.getLogger(RDF2Graph.class);

	public static String getRootSubjectLabel(org.apache.jena.rdf.model.Statement rdfStatement, String type){
		String subjectValue = rdfStatement.getSubject().toString();
		String predicate = rdfStatement.getPredicate().toString();
		String label = null;
		if(predicate.equals(RDF.TYPE.toString())){
			RDFNode object = rdfStatement.getObject();
			if(object.isURIResource()){
				if(object.toString().equals(type)){
					label = subjectValue;
					logger.info("Printing root label:" + label);
				}
			}
		}
		return label;
	}

	public static Graph convertRDFStatement2Graph(Statement rdfStatement, Graph graph) {
		Value subjectValue = rdfStatement.getSubject();
		IRI property = rdfStatement.getPredicate();
		Value objectValue = rdfStatement.getObject();
		updateGraph(subjectValue, property, objectValue, graph);
		return graph;
	}

	private static void updateGraph(Value subjectValue, IRI property, Value objectValue, Graph graph) {
		Vertex s = getExistingVertexOrAdd(subjectValue.toString(), graph);
		
		if (objectValue instanceof Literal) {
			Literal literal = (Literal)objectValue;
			s.property(property.toString(), literal.getLabel());

		} else if (objectValue instanceof IRI) {
			IRI objectIRI = (IRI)objectValue;
			Vertex o = getExistingVertexOrAdd(objectIRI.toString(), graph);
			s.addEdge(property.toString(), o);
			
		} else if (objectValue instanceof BNode) {
			BNode objectBNode = (BNode)objectValue;
			Vertex o = getExistingVertexOrAdd(objectBNode.toString(), graph);		
			s.addEdge(property.toString(), o);
			
		}
	}
	
	private static Vertex getExistingVertexOrAdd(String label, Graph graph){
		GraphTraversalSource t = graph.traversal();
		GraphTraversal<Vertex, Vertex> traversal = t.V();
		GraphTraversal<Vertex, Vertex> hasLabel = traversal.hasLabel(label);
		if(hasLabel.hasNext()){
			return hasLabel.next();
		} else {
			return graph.addVertex(T.label,label);
		}
	}
	
	public static org.apache.jena.rdf.model.Model convertRDFModel2Jena(Model rdf4jModel) {
		org.apache.jena.rdf.model.Model jenaModel = ModelFactory.createDefaultModel();
		rdf4jModel.forEach(stmt -> jenaModel.add(convert(stmt)));
		return jenaModel;
	}

	private static org.apache.jena.rdf.model.Statement convert(Statement rdf4jStatement) {
		Value subjectValue = rdf4jStatement.getSubject();
		IRI property = rdf4jStatement.getPredicate();
		Value objectValue = rdf4jStatement.getObject();
		return null;
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
