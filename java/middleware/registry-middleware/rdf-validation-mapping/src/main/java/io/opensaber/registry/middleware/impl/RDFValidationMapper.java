package io.opensaber.registry.middleware.impl;

import java.io.IOException;
import java.util.Map;


import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;

public class RDFValidationMapper implements BaseMiddleware{
	
	private static final String RDF_DATA_IS_MISSING = "RDF Data is missing";
	private static final String RDF_DATA_IS_INVALID = "RDF Data is invalid";
	private static final String VALIDATION_IS_MISSING = "Validation is missing";
	private static final String VALIDATION_MISSING_FOR_TYPE = "Validation missing for type";

	private Map<String,String> shapeTypeMap;

	public RDFValidationMapper(Map<String,String> typeValidationMap){
		if(typeValidationMap.size()>0){
			initializeShapeMap(typeValidationMap);
		}
	}

	private void initializeShapeMap(Map<String,String> typeValidationMap){
		shapeTypeMap = typeValidationMap;
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
		for(Map.Entry<String, String> map: shapeTypeMap.entrySet()){
			String key = map.getKey();
			StmtIterator iter = filterStatement(null, RDF.type, key, inputRdf);
			String value = map.getValue();
			if(value==null){
				throw new MiddlewareHaltException(this.getClass().getName()+VALIDATION_MISSING_FOR_TYPE);
			}
			while(iter.hasNext()){
				Resource subjectResource = ResourceFactory.createResource(value);
				Property predicate = ResourceFactory.createProperty(Constants.TARGET_NODE_IRI);
				model.add(subjectResource,predicate, iter.next().getSubject());
			}
		}
		return model;
	}

	
	private StmtIterator filterStatement(String subject, Property predicate, String object, Model resultModel){
		Resource subjectResource = subject!=null? ResourceFactory.createResource(subject) : null;
		RDFNode objectResource = object!=null? ResourceFactory.createResource(object) : null;
		StmtIterator iter = resultModel.listStatements(subjectResource, predicate, objectResource);
		return iter;
	}

}
