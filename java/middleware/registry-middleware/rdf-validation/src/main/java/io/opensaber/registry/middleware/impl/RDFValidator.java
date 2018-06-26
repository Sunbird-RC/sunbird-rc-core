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

import es.weso.schema.Schema;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.Validator;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;
import io.opensaber.pojos.ValidationResponse;

public class RDFValidator implements Middleware{

	private static final String RDF_DATA_IS_MISSING = "RDF Data is missing!";
	private static final String RDF_DATA_IS_INVALID = "Data validation failed!";
	private static final String RDF_VALIDATION_MAPPING_IS_INVALID = "RDF validation mapping is invalid!";
	private static final String RDF_VALIDATION_MAPPING_MISSING = "RDF validation mapping is missing!";
    private static final String SCHEMA_IS_NULL = "Schema for validation is missing";
	private static final String INVALID_REQUEST_PATH = "Request URL is invalid";
	private static final String ADD_REQUEST_PATH = "/add";

	private static final String VALIDATION_IS_MISSING = "Validation is missing";
	private static final String VALIDATION_MISSING_FOR_TYPE = "Validation missing for type";
	private static final String SX_SHAPE_IRI = "http://shex.io/ns/shex#Shape";
	private static final String SX_EXPRESSION_IRI = "http://shex.io/ns/shex#expression";
	private static final String SX_EXPRESSIONS_IRI = "http://shex.io/ns/shex#expressions";
	private static final String SX_VALUES_IRI = "http://shex.io/ns/shex#values";
	private static final String SX_VALUE_EXPR_IRI = "http://shex.io/ns/shex#valueExpr";
	private static final String FORMAT = "JSON-LD";
	
	private Map<String,String> shapeTypeMap;
	private Schema schemaForCreate;
	private Schema schemaForUpdate;
	
	public RDFValidator(Schema schemaForCreate, Schema schemaForUpdate) {
		this.schemaForCreate = schemaForCreate;
		this.schemaForUpdate = schemaForUpdate;
		this.shapeTypeMap = getShapeMap(RDF.type, SX_SHAPE_IRI);
	}
	
	public Map<String,String> getShapeTypeMap(){
		return shapeTypeMap;
	}

	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		Object RDF = mapData.get(Constants.RDF_OBJECT);
		Object method = mapData.get(Constants.METHOD_ORIGIN);
		//Object validationRDF = mapData.get(Constants.RDF_VALIDATION_MAPPER_OBJECT);
		if (RDF == null) {
			throw new MiddlewareHaltException(RDF_DATA_IS_MISSING);
		}else if (!(RDF instanceof Model)) {
			throw new MiddlewareHaltException(RDF_DATA_IS_INVALID);
		}else if (method == null){
			throw new MiddlewareHaltException(INVALID_REQUEST_PATH);
		}else if (schemaForCreate == null || schemaForUpdate == null) {
			throw new MiddlewareHaltException(SCHEMA_IS_NULL);
		}else if(shapeTypeMap == null){
			throw new MiddlewareHaltException(this.getClass().getName()+VALIDATION_IS_MISSING); 
		} else {
			Schema schema = null;
			Model validationRdf = generateShapeModel((Model)RDF);
			mergeModels((Model) RDF, (Model) validationRdf);
			ValidationResponse validationResponse = null;
			if(ADD_REQUEST_PATH.equals((String)method)){
				schema = schemaForCreate;
			} else {
				schema = schemaForUpdate;
			}
			Validator validator = new ShaclexValidator(schema, (Model) validationRdf);
			validationResponse = validator.validate();
			mapData.put(Constants.RDF_VALIDATION_OBJECT, validationResponse);
			return mapData;
		}
	}

	public Map<String, Object> next(Map<String, Object> mapData) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	private void mergeModels(Model RDF, Model validationRDF){
		if(validationRDF!=null){
			validationRDF.add(RDF.listStatements());
		}
	}
	
	
	private Model generateShapeModel(Model inputRdf) throws MiddlewareHaltException {
		Model model = ModelFactory.createDefaultModel();
		List<Resource> labelNodes = RDFUtil.getRootLabels(inputRdf);
		if (labelNodes.size() != 1) {
			throw new MiddlewareHaltException(this.getClass().getName() + RDF_DATA_IS_INVALID);
		}
		Resource target = labelNodes.get(0);
		List<String> typeList = RDFUtil.getTypeForSubject(inputRdf, target);
		if (typeList.size() != 1) {
			throw new MiddlewareHaltException(this.getClass().getName() + RDF_DATA_IS_INVALID);
		}
		String targetType = typeList.get(0);
		String shapeName = shapeTypeMap.get(targetType);
		if (shapeName == null) {
			throw new MiddlewareHaltException(this.getClass().getName() + VALIDATION_MISSING_FOR_TYPE);
		}

		Resource subjectResource = ResourceFactory.createResource(shapeName);
		Property predicate = ResourceFactory.createProperty(Constants.TARGET_NODE_IRI);
		model.add(subjectResource, predicate, target);
		return model;
	}

	/**
	 * This method generates a shapemap which contains mappings between each entity type and the corresponding
	 * shape that the validations should target. Here we first filter out all the shape resources from the validationConfig.
	 * Then we iterate through the list of shape resources and do a bunch of filtering from the validationConfig
	 * based on a few predicates to finally arrive at the type for which the shape is targeted.
	 * @param predicate
	 * @param object
	 * @param validationConfig is the rdf model format of the Schema file used for validations
	 * @return
	 */
	private Map<String,String> getShapeMap(Property predicate, String object){
		Map<String,String> shapeMap = new HashMap<String, String>();
		Model validationConfig = getValidationConfigModel();
		List<Resource> shapeList = RDFUtil.getListOfSubjects(predicate, object, validationConfig);
		for(Resource shape: shapeList){
			RDFNode node = getObjectAfterFilter(shape, SX_EXPRESSION_IRI, validationConfig);
			RDFNode firstNode = getObjectAfterFilter(node, SX_EXPRESSIONS_IRI, validationConfig);
			RDFNode secondNode = getObjectAfterFilter(firstNode, RDF.first.getURI(), validationConfig);
			RDFNode thirdNode = getObjectAfterFilter(secondNode, SX_VALUES_IRI, validationConfig);
			if(thirdNode == null){
				thirdNode = getObjectAfterFilter(secondNode, SX_VALUE_EXPR_IRI, validationConfig);
			}
			RDFNode fourthNode = getObjectAfterFilter(thirdNode, SX_VALUES_IRI, validationConfig);
			RDFNode typeNode = getObjectAfterFilter(fourthNode, RDF.first.getURI(), validationConfig);
			if(typeNode!=null){
				shapeMap.put(typeNode.toString(), shape.toString());
			}
		}
		return shapeMap;
	}
	
	private RDFNode getObjectAfterFilter(RDFNode node, String predicate, Model validationConfig){
			Property property = ResourceFactory.createProperty(predicate);
			List<RDFNode> nodeList = RDFUtil.getListOfObjectNodes((Resource)node, property,validationConfig);
			if(nodeList.size() != 0){
				return nodeList.get(0);
			}
			return null;
	}
	
	private Model getValidationConfigModel(){
		return RDFUtil.getRdfModelBasedOnFormat(schemaForUpdate.serialize(FORMAT).right().get(), FORMAT);
	}

}
