package io.opensaber.registry.middleware.impl;


import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import es.weso.schema.Schema;
import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;
import io.opensaber.pojos.ValidationResponse;

public class RDFValidator implements BaseMiddleware{

	private static final String RDF_DATA_IS_MISSING = "RDF Data is missing!";
	private static final String RDF_DATA_IS_INVALID = "RDF Data is invalid!";
	private static final String RDF_VALIDATION_MAPPING_IS_INVALID = "RDF validation mapping is invalid!";
	private static final String RDF_VALIDATION_MAPPING_MISSING = "RDF validation mapping is missing!";
	//private static final String RDF_VALIDATION_MAPPING_NULL = "RDF validation mapping is null!";

	private String schemaFileName;
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";

	public RDFValidator(String schemaFileName) {
		this.schemaFileName = schemaFileName;
	}

	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		Object RDF = mapData.get(Constants.RDF_OBJECT);
		Object validationRDF = mapData.get(Constants.RDF_VALIDATION_MAPPER_OBJECT);
		if (RDF == null) {
			throw new MiddlewareHaltException(RDF_DATA_IS_MISSING);
		} else if (validationRDF == null) {
			throw new MiddlewareHaltException(RDF_VALIDATION_MAPPING_MISSING);
		} else if (!(RDF instanceof Model)) {
			throw new MiddlewareHaltException(RDF_DATA_IS_INVALID);
		} else if (!(validationRDF instanceof Model)) {
			throw new MiddlewareHaltException(RDF_VALIDATION_MAPPING_IS_INVALID);
		} else {
			ShaclexValidator validator = new ShaclexValidator();
			Schema schema = validator.readSchema(schemaFileName, SCHEMAFORMAT, PROCESSOR);
			mergeModels((Model) RDF, (Model) validationRDF);
			ValidationResponse validationResponse = validator.validate((Model) validationRDF, schema);
			mapData.put(Constants.RDF_VALIDATION_OBJECT, validationResponse);
			/*
			if (!validationResponse.isValid()) {
				throw new MiddlewareHaltException(RDF_DATA_IS_INVALID);
			}
			*/
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

}
