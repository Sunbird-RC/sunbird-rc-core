package io.opensaber.utils.converters;

import java.util.Iterator;
import java.util.Stack;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.*;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.namespace.QName;


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
			String datatype = literal.getDatatype().toString();
			logger.info("TYPE saved is "+datatype);
			s.property(property.toString(), literal.getLabel()).property("@type",datatype);
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
		logger.info("Vertex "+s.label());
		ValueFactory vf = SimpleValueFactory.getInstance();
		logger.info("ADDING it as Subject");
		builder.subject(s.label());
		Iterator<VertexProperty<String>> propertyIter = s.properties();
		while (propertyIter.hasNext()){
			VertexProperty<String> property = propertyIter.next();
			logger.info("ADDING Property"+property.label()+": "+property.value());
			String literal = property.value();
			Property<Object> metaProperty = property.property("@type");
			String type = null;
			if(metaProperty.isPresent()){
			    type = metaProperty.value().toString();
            }
			Object object = literal;
			logger.info("TYPE is: "+type);
			if(type!=null){
                switch(type){
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#boolean":
                        object=XMLDatatypeUtil.parseBoolean(literal);
                        break;
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#byte":
                        object=XMLDatatypeUtil.parseByte(literal);
                        break;
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#short":
                        object=XMLDatatypeUtil.parseShort(literal);
                        break;
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#int":
                        object=XMLDatatypeUtil.parseInt(literal);
                        break;
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#long":
                        object=XMLDatatypeUtil.parseLong(literal);
                        break;
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#float":
                        object=XMLDatatypeUtil.parseFloat(literal);
                        break;
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#double":
                        object=XMLDatatypeUtil.parseDouble(literal);
                        break;
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#integer":
                        object=XMLDatatypeUtil.parseInteger(literal);
                        break;
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#decimal":
                        object=XMLDatatypeUtil.parseDecimal(literal);
                        break;
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#dateTime":
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#time":
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#date":
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#gYearMonth":
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#gMonthDay":
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#gYear":
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#gMonth":
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#gDay":
                        object=XMLDatatypeUtil.parseCalendar(literal);
                        break;
                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#duration":
                        object=XMLDatatypeUtil.parseDuration(literal);
                        break;
                }

            }
			builder.add(property.label(), object);
		}
		Iterator<Edge> edgeIter = s.edges(Direction.OUT);
		Edge edge;
		Stack<Vertex> vStack = new Stack<Vertex>();
		while(edgeIter.hasNext()){
			edge = edgeIter.next();
			s = edge.inVertex();
//			builder.add(edge.label(), bNode);
			Resource node;
			if(s.label().startsWith("_:")){
				logger.info("ADDING NODE - BLANK");
				node = vf.createBNode();
			} else {
				node = vf.createIRI(s.label());
				logger.info("ADDING NODE - IRI");
			}
			builder.add(edge.label(), node);
			vStack.push(s);
		}
		Iterator<Vertex> vIterator = vStack.iterator();
		while(vIterator.hasNext()){
			s = vIterator.next();
			extractModelFromVertex(builder,s);
		}
	}
}
