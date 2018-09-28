package io.opensaber.registry.middleware.impl;

import es.weso.schema.Schema;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SignaturePresenceValidator implements Middleware{

	private static final String RDF_DATA_IS_MISSING = "RDF Data is missing!";
	private static final String RDF_DATA_IS_INVALID = "Data validation failed!";
	private static final String SCHEMA_IS_NULL = "Schema for validation is missing";
	private static final String SHAPE_EXPRESSION_IRI = "http://shex.io/ns/shex#expression";
	private static final String SHAPE_EXPRESSIONS_IRI = "http://shex.io/ns/shex#expressions";
	private static final String SHAPE_PREDICATE_IRI = "http://shex.io/ns/shex#predicate";
	private static final String JSON_LD_FORMAT = "JSON-LD";
	private static final String SIGNATURE_NOT_FOUND = "Signature not found for attribute %s";

	private Schema schemaForCreate;
	private String registryContext;
	private String signatureConfigName;
	private String registrySystemBase;
	private Model schemaConfig;
	private List<String> signatureTypes = new ArrayList<String>();
	private List<String> signatureAttributes;

    // TODO: Instead of passing the ShapeType everytime, there could be a good reason
    // to read all the shapes at once and then start validating against what was read.
    public SignaturePresenceValidator(Schema schemaForCreate, String registryContext,
                                      String registrySystemBase, String signatureConfigName,
                                      Map<String, String> shapeTypeMap, Model schemaConfig) {
		this.schemaForCreate = schemaForCreate;
		this.registryContext = registryContext;
		this.signatureConfigName = signatureConfigName;
		this.schemaConfig = schemaConfig;
		this.registrySystemBase = registrySystemBase;
		shapeTypeMap.forEach((type, shape)-> {
			if(shape.equals(registryContext+signatureConfigName)){
				signatureTypes.add(type);
			}
		});
		signatureAttributes = getSignatureAttributes();
	}

	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		Object RDF = mapData.get(Constants.RDF_OBJECT);
		if (RDF == null) {
			throw new MiddlewareHaltException(RDF_DATA_IS_MISSING);
		}else if (!(RDF instanceof Model)) {
			throw new MiddlewareHaltException(RDF_DATA_IS_INVALID);
		}else if (schemaForCreate == null) {
			throw new MiddlewareHaltException(SCHEMA_IS_NULL);
		}else {
			validateSignature((Model)RDF);
			return mapData;
		}
	}

	public Map<String, Object> next(Map<String, Object> mapData) throws IOException {
		return null;
	}

	private List<String> getSignatureAttributes(){
		return getAttributeListForShape(registryContext+signatureConfigName, getSignatureConfigModel());
	}

	private Model getSignatureConfigModel(){
		return RDFUtil.getRdfModelBasedOnFormat(schemaForCreate.serialize(JSON_LD_FORMAT).right().get(), JSON_LD_FORMAT);
	}

	/**
	 * This method takes any shape as input and the schema which contains validation
	 * and returns a list of attributes specified for that shape. 
	 * @param shape
	 * @param signatureConfigModel
	 * @return
	 */
	private List<String> getAttributeListForShape(String shape, Model signatureConfigModel){
		List<String> attributeList = new ArrayList<String>();
		Resource shapeResource = ResourceFactory.createResource(shape);
		//Getting the object by filtering the shape and the IRI
        RDFNode node = RDFUtil.getFirstObject(shapeResource, SHAPE_EXPRESSION_IRI, signatureConfigModel);
		//Getting the object by filtering the node and the IRI
        RDFNode firstNode = RDFUtil.getFirstObject((Resource) node, SHAPE_EXPRESSIONS_IRI, signatureConfigModel);
		addAttributesForShape(firstNode, signatureConfigModel, attributeList);
		return attributeList;
	}

	/**
	 * This method adds attributes of a shape to the attributeList
	 * @param firstNode
	 * @param signatureConfigModel
	 * @param attributeList
	 */
	private void addAttributesForShape(RDFNode firstNode, Model signatureConfigModel, List<String> attributeList){
		//Getting object nodes by filtering based on different predicates iteratively till all attributes for the shape are filtered
        RDFNode secondNode = RDFUtil.getFirstObject((Resource) firstNode, RDF.rest.getURI(), signatureConfigModel);
		if(!secondNode.equals(RDF.nil)){
            RDFNode thirdNode = RDFUtil.getFirstObject((Resource) secondNode, RDF.first.getURI(), signatureConfigModel);
            RDFNode fourthNode = RDFUtil.getFirstObject((Resource) thirdNode, SHAPE_PREDICATE_IRI, signatureConfigModel);

            attributeList.add(fourthNode.toString());
			addAttributesForShape(secondNode, signatureConfigModel, attributeList);
		}
	}

    private void validateSignature(Model rdfModel) throws MiddlewareHaltException {
		Property property = ResourceFactory.createProperty(registrySystemBase + Constants.SIGNED_PROPERTY);
        StmtIterator rdfIter = rdfModel.listStatements();
		Property prop = ResourceFactory.createProperty(registryContext+Constants.SIGNATURE_FOR);
		TypeMapper tm = TypeMapper.getInstance();
		//This is the datatype for the signatureFor attribute
		RDFDatatype rdt = tm.getSafeTypeByName(XMLConstants.W3C_XML_SCHEMA_NS_URI+"#anyURI");
		while(rdfIter.hasNext()){
			Statement s = rdfIter.next();
			RDFNode rNode = s.getObject();
			String rNodeStr = rNode.toString();
			Property predicate = s.getPredicate(); 
			String predicateStr = predicate.toString();
			RDFNode propertyResource = ResourceFactory.createResource(predicateStr);
			if(!rNode.isAnon() && !predicate.equals(RDF.type) && !signatureAttributes.contains(predicateStr) 
					&& schemaConfig.contains(null, property, propertyResource)){
				//Filtering statements based on the signatureFor attribute to check with signature exists for each attribute
                ResIterator subjectIter = rdfModel.listSubjectsWithProperty(prop, ResourceFactory.createTypedLiteral(predicateStr, rdt));
				if(!subjectIter.hasNext()){
					throw new MiddlewareHaltException(String.format(SIGNATURE_NOT_FOUND, predicateStr));
				}else{
					boolean attributeSignatureFound = false;
                    //subjectIter.filterDrop()
					while(subjectIter.hasNext()){
						Resource subject = subjectIter.next();
                        attributeSignatureFound = rdfModel.contains(s.getSubject(),
                                ResourceFactory.createProperty(registryContext + Constants.SIGNATURES), subject);
					}
					if(!attributeSignatureFound){
						throw new MiddlewareHaltException(String.format(SIGNATURE_NOT_FOUND, predicateStr));
					}
				}
			}else if(predicate.equals(RDF.type) && !signatureTypes.contains(rNodeStr) 
					&& schemaConfig.contains(null, property, rNode)){
                NodeIterator nodeIter = rdfModel.listObjectsOfProperty(s.getSubject(), ResourceFactory.createProperty(registryContext + Constants.SIGNATURES));
				boolean entitySignatureFound = false;
                while (nodeIter.hasNext() && !entitySignatureFound) {
					RDFNode node = nodeIter.next();
					//Filtering statements to check if signature exists for entity
                    entitySignatureFound = rdfModel.contains((Resource) node, prop, ResourceFactory.createTypedLiteral(rNodeStr, rdt));
                }

				if(!entitySignatureFound){
					throw new MiddlewareHaltException(String.format(SIGNATURE_NOT_FOUND, s.getObject().toString()));
				}	
			}
		}
	}	
}
