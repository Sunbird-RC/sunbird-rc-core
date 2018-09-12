package io.opensaber.registry.middleware.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import es.weso.schema.Schema;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.Validator;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;
import io.opensaber.pojos.ValidationResponse;

public class SignaturePresenceValidator implements Middleware{

	private static final String RDF_DATA_IS_MISSING = "RDF Data is missing!";
	private static final String RDF_DATA_IS_INVALID = "Data validation failed!";
	private static final String SCHEMA_IS_NULL = "Schema for validation is missing";
	private static final String SX_EXPRESSION_IRI = "http://shex.io/ns/shex#expression";
	private static final String SX_EXPRESSIONS_IRI = "http://shex.io/ns/shex#expressions";
	private static final String SX_PREDICATE_IRI = "http://shex.io/ns/shex#predicate";
	private static final String FORMAT = "JSON-LD";
	private static final String SIGNATURE_NOT_FOUND = "Signature not found for attribute %s";

	private Schema schemaForCreate;
	private String registryContext;
	private String signatureConfigName;

	public SignaturePresenceValidator(Schema schemaForCreate, String registryContext, String signatureConfigName) {
		this.schemaForCreate = schemaForCreate;
		this.registryContext = registryContext;
		this.signatureConfigName = signatureConfigName;
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
		// TODO Auto-generated method stub
		return null;
	}

	private RDFNode getObjectAfterFilter(RDFNode node, String predicate, Model validationConfig){
		Property property = ResourceFactory.createProperty(predicate);
		List<RDFNode> nodeList = RDFUtil.getListOfObjectNodes((Resource)node, property,validationConfig);
		if(nodeList.size() != 0){
			return nodeList.get(0);
		}
		return null;
	}

	private List<String> getSignatureAttributes(){
		return getAttributeListForShape(registryContext+signatureConfigName, getSignatureConfigModel());
	}

	private Model getSignatureConfigModel(){
		return RDFUtil.getRdfModelBasedOnFormat(schemaForCreate.serialize(FORMAT).right().get(), FORMAT);
	}

	private List<String> getAttributeListForShape(String shape, Model signatureConfigModel){
		List<String> attributeList = new ArrayList<String>();
		Resource shapeResource = ResourceFactory.createResource(shape);
		RDFNode node = getObjectAfterFilter(shapeResource, SX_EXPRESSION_IRI, signatureConfigModel);
		RDFNode firstNode = getObjectAfterFilter(node, SX_EXPRESSIONS_IRI, signatureConfigModel);
		getOtherAttributes(firstNode, signatureConfigModel, attributeList);
		return attributeList;
	}

	private void getOtherAttributes(RDFNode firstNode, Model signatureConfigModel, List<String> attributeList){
		RDFNode secondNode = getObjectAfterFilter(firstNode, RDF.rest.getURI(), signatureConfigModel);
		if(!secondNode.equals(RDF.nil)){
			RDFNode thirdNode = getObjectAfterFilter(secondNode, RDF.first.getURI(), signatureConfigModel);
			RDFNode fourthNode = getObjectAfterFilter(thirdNode, SX_PREDICATE_IRI, signatureConfigModel);
			attributeList.add(fourthNode.toString());
			getOtherAttributes(secondNode, signatureConfigModel, attributeList);
		}
	}
	private void validateSignature(Model rdf) throws MiddlewareHaltException{
		StmtIterator iter = rdf.listStatements();
		Property prop = ResourceFactory.createProperty(registryContext+Constants.SIGNATURE_FOR);
		List<String> predicateList = new ArrayList<String>();
		List<String> signatureAttributes = getSignatureAttributes();
		while(iter.hasNext()){
			Statement s = iter.next();
			RDFNode rNode = s.getObject();
			if(!rNode.isAnon() && !s.getPredicate().equals(RDF.type)){
				String predicate = s.getPredicate().toString();
				if(!signatureAttributes.contains(predicate)){
					predicateList.add(predicate);
				}
			}
		}

		for(String predicate: predicateList){
			StmtIterator signatureIter = rdf.listStatements(null, prop, (RDFNode)null);
			boolean signatureFoundFlag = false;
			while(signatureIter.hasNext()){
				Statement signatureStmt = signatureIter.next();
				RDFNode signatureRNode = signatureStmt.getObject();
				if(signatureRNode.isLiteral()){
					String signatureForValue = signatureRNode.asLiteral().getLexicalForm();
					if(signatureForValue.equalsIgnoreCase(predicate)){
						signatureFoundFlag = true;
					}
				}
			}
			if(!signatureFoundFlag){
				throw new MiddlewareHaltException(String.format(SIGNATURE_NOT_FOUND, predicate));
			}
		}
	}

}
