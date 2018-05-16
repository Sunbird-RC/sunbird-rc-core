package io.opensaber.registry.middleware.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;

public class RDFValidationMapper implements BaseMiddleware{
	
	private static final String RDF_DATA_IS_MISSING = "RDF Data is missing";
	private static final String RDF_DATA_IS_INVALID = "RDF Data is invalid";
	private static final String VALIDATION_IS_MISSING = "Validation is missing";
	private static final String VALIDATION_MISSING_FOR_TYPE = "Validation missing for type";
	private static final String SX_SHAPE_NAMESPACE = "http://shex.io/ns/shex#Shape";

	private Map<String,String> shapeTypeMap;

	public RDFValidationMapper(Model validationConfig){
		if(validationConfig!=null){
			initializeShapeMap(validationConfig);
		}
	}

	private void initializeShapeMap(Model validationConfig){
		shapeTypeMap = getShapeMap(RDF.type, SX_SHAPE_NAMESPACE, validationConfig);
	}

	public Map<String,String> getShapeTypeMap(){
		return shapeTypeMap;
	}



	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		Object RDF = mapData.get(Constants.RDF_OBJECT);
		if (RDF==null) {
			throw new MiddlewareHaltException(this.getClass().getName()+RDF_DATA_IS_MISSING); 
		}else if(shapeTypeMap == null){
			throw new MiddlewareHaltException(this.getClass().getName()+VALIDATION_IS_MISSING); 
		}else if (RDF instanceof Model) {
			Model validationRdf = generateShapeModel((Model)RDF);
			mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, validationRdf);
			return mapData;
		} else {
			throw new MiddlewareHaltException(this.getClass().getName()+RDF_DATA_IS_INVALID);
		}
	}

	public Map<String, Object> next(Map<String, Object> mapData) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public Model generateShapeModel(Model inputRdf) throws MiddlewareHaltException{
		Model model = ModelFactory.createDefaultModel();
		List<Resource> labelNodes = RDFUtil.getRootLabels(inputRdf);
		if(labelNodes.size() == 0 || labelNodes.size()>1){
			throw new MiddlewareHaltException(this.getClass().getName()+RDF_DATA_IS_INVALID);
		}
		Resource target = labelNodes.get(0);
		List<String> typeList = RDFUtil.getTypeForSubject(inputRdf, target);
		if(typeList.size() == 0 || typeList.size()>1){
			throw new MiddlewareHaltException(this.getClass().getName()+RDF_DATA_IS_INVALID);
		}
		String targetType = typeList.get(0);
		String shapeName = shapeTypeMap.get(targetType);
		if(shapeName == null){
			throw new MiddlewareHaltException(this.getClass().getName()+VALIDATION_MISSING_FOR_TYPE);
		}

		Resource subjectResource = ResourceFactory.createResource(shapeName);
		Property predicate = ResourceFactory.createProperty(Constants.TARGET_NODE_IRI);
		model.add(subjectResource,predicate, target);
		return model;
	}

	private Map<String,String> getShapeMap(Property predicate, String object, Model validationConfig){
		Map<String,String> shapeMap = new HashMap<String, String>();
		List<Resource> shapeList = RDFUtil.getListOfSubjects(predicate, object, validationConfig);
		for(Resource subject: shapeList){
			Property property1 = ResourceFactory.createProperty("http://shex.io/ns/shex#expression");
			List<RDFNode> rdfNodeList = RDFUtil.getListOfObjectNodes(subject, property1, validationConfig);
			RDFNode firstNode = getIteratorAfterFilter(rdfNodeList.get(0), "http://shex.io/ns/shex#expressions", validationConfig);
			RDFNode secondNode = getIteratorAfterFilter(firstNode, "http://www.w3.org/1999/02/22-rdf-syntax-ns#first", validationConfig);
			RDFNode thirdNode = getIteratorAfterFilter(secondNode, "http://shex.io/ns/shex#values", validationConfig);
			if(thirdNode == null){
				thirdNode = getIteratorAfterFilter(secondNode, "http://shex.io/ns/shex#valueExpr", validationConfig);
			}
			RDFNode fourthNode = getIteratorAfterFilter(thirdNode, "http://shex.io/ns/shex#values", validationConfig);
			RDFNode fifthNode = getIteratorAfterFilter(fourthNode, "http://www.w3.org/1999/02/22-rdf-syntax-ns#first", validationConfig);
			if(fifthNode!=null){
				shapeMap.put(fifthNode.toString(), subject.toString());
			}
		}
		return shapeMap;
	}
	
	private RDFNode getIteratorAfterFilter(RDFNode node, String predicate, Model validationConfig){
			Property property = ResourceFactory.createProperty(predicate);
			List<RDFNode> nodeList = RDFUtil.getListOfObjectNodes((Resource)node, property,validationConfig);
			if(nodeList.size() != 0){
				return nodeList.get(0);
			}
			return null;
	}
}
