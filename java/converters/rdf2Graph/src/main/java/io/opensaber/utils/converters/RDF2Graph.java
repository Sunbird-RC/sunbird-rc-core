package io.opensaber.utils.converters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

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
					logger.debug("Printing root label:" + label);
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
			logger.debug("TYPE saved is "+datatype);
			VertexProperty vp = s.property(property.toString());
			if(vp.isPresent()){
				Object value = vp.value();
				List valueList = new ArrayList();
				if(value instanceof List){
					valueList = (List)value;
				} else{
					String valueStr = (String)value;
					valueList.add(valueStr);
				}
				valueList.add(literal.getLabel());
				s.property(property.toString(), valueList).property("@type",datatype);

			}else{
				s.property(property.toString(), literal.getLabel()).property("@type",datatype);
			}
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
		logger.debug("Vertex "+s.label());
		ValueFactory vf = SimpleValueFactory.getInstance();
		logger.debug("ADDING it as Subject");
		builder.subject(s.label());
		Iterator<VertexProperty<String>> propertyIter = s.properties();
		while (propertyIter.hasNext()) {
			VertexProperty property = propertyIter.next();
			logger.debug("ADDING Property " + property.label() + ": " + property.value());
			Object object = property.value();
			Property<Object> metaProperty = property.property("@type");
			String type = null;
			if (metaProperty.isPresent()) {
				type = metaProperty.value().toString();
			}
			logger.debug("TYPE is: " + type);
			//Object object = literal;
			if (object instanceof List) {
				for (Object ob : (List) object) {
					String literal = (String) ob;
					Object finalObj = literal;
					if (type != null) {
						finalObj = matchAndAddStatements(type, literal, vf);
					}
					builder.add(property.label(), finalObj);

				}
			} else if(object instanceof String[]) {
				for (String literal : (String[]) object) {
					Object finalObj = literal;
					if (type != null) {
						finalObj = matchAndAddStatements(type, literal, vf);
					}
					builder.add(property.label(), finalObj);

				}
			} else {
				String literal = (String) object;
				Object finalObj = literal;
				if (type != null) {
					finalObj = matchAndAddStatements(type, literal, vf);
				}
				builder.add(property.label(), finalObj);
			}

			logger.debug("OBJECT ADDED is " + object + "-" + object.getClass().getName());
			//builder.add(property.label(), object);

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
				logger.debug("ADDING NODE - BLANK");
				node = vf.createBNode();
			} else {
				node = vf.createIRI(s.label());
				logger.debug("ADDING NODE - IRI");
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

	private static Object matchAndAddStatements(String type, String literal, ValueFactory vf){
		Object object = literal;
		switch(type){
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#boolean":
			logger.debug("Found boolean");

		object=vf.createLiteral(XMLDatatypeUtil.parseBoolean(literal));
		break;
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#byte":
			object=vf.createLiteral(XMLDatatypeUtil.parseByte(literal));
		logger.debug("Found byte");
		break;
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#short":
			object=vf.createLiteral(XMLDatatypeUtil.parseShort(literal));
		logger.debug("Found short");
		break;
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#int":
			object=vf.createLiteral(XMLDatatypeUtil.parseInt(literal));
		logger.debug("Found int");
		break;
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#long":
			object=vf.createLiteral(XMLDatatypeUtil.parseLong(literal));
		logger.debug("Found long");
		break;
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#float":
			object=vf.createLiteral(XMLDatatypeUtil.parseFloat(literal));
		logger.debug("Found float");
		break;
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#double":
			object=vf.createLiteral(XMLDatatypeUtil.parseDouble(literal));
		logger.debug("Found double");
		break;
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#integer":
			object=vf.createLiteral(XMLDatatypeUtil.parseInteger(literal));
		logger.debug("Found integer");
		break;
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#decimal":
			object=vf.createLiteral(XMLDatatypeUtil.parseDecimal(literal));
		logger.debug("Found decimal");
		break;
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#dateTime":
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#time":
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#date":
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#gYearMonth":
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#gMonthDay":
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#gYear":
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#gMonth":
		case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#gDay":
			object=vf.createLiteral(XMLDatatypeUtil.parseCalendar(literal));
		logger.debug("Found date");
		break;
		//                    case XMLConstants.W3C_XML_SCHEMA_NS_URI+"#duration":
		//                        object=vf.createLiteral(XMLDatatypeUtil.parseDuration(literal));
		//                        logger.info("Found duration");
		//                        break;
		}
		return object;
	}
}
