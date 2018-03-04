package io.opensaber.registry.middleware.impl;


import java.io.IOException;
import java.util.Map;

import org.apache.jena.rdf.model.Model;

import es.weso.schema.Result;
import es.weso.schema.Schema;
import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;

public class RDFValidator implements BaseMiddleware{

	private static final String RDF_DATA_IS_MISSING = "RDF Data is missing!";
	private static final String RDF_DATA_IS_INVALID = "RDF Data is invalid!";

	private String schemaFileName;
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";

	public RDFValidator(String schemaFileName) {
		this.schemaFileName = schemaFileName;
	}

	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		Object RDF = mapData.get(Constants.RDF_OBJECT);
		Object validationRDF = mapData.get(Constants.RDF_VALIDATION_MAPPER_OBJECT);
		if (RDF==null) {
			throw new MiddlewareHaltException(this.getClass().getName()+RDF_DATA_IS_MISSING); 
		} else if (RDF instanceof Model) {
			ShaclexValidator validator = new ShaclexValidator();

			Schema schema = validator.readSchema(schemaFileName, SCHEMAFORMAT, PROCESSOR);
			Model rdfWithValidations = mergeModels((Model)RDF, (Model)validationRDF);
			Result validationResult = validator.validate(rdfWithValidations, schema);

			mapData.put(Constants.RDF_VALIDATION_OBJECT, validationResult);
			if(!validationResult.isValid()){
				throw new MiddlewareHaltException(this.getClass().getName()+RDF_DATA_IS_INVALID);
			}
			return mapData;
		} else {
			throw new MiddlewareHaltException(this.getClass().getName()+"RDF Data is invalid!");
		}
	}

	public Map<String, Object> next(Map<String, Object> mapData) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public Model mergeModels(Model RDF, Model validationRDF){
		if(validationRDF!=null){
			RDF.add(validationRDF.listStatements());
		}
		return RDF;
	}

}
