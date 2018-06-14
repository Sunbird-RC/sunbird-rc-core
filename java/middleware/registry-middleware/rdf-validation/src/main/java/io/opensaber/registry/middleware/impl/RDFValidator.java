package io.opensaber.registry.middleware.impl;

import java.io.IOException;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import es.weso.schema.Schema;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.Validator;
import io.opensaber.registry.middleware.util.Constants;
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

	private Schema schemaForCreate;
	private Schema schemaForUpdate;
	
	public RDFValidator(Schema schemaForCreate, Schema schemaForUpdate) {
		this.schemaForCreate = schemaForCreate;
		this.schemaForUpdate = schemaForUpdate;
	}

	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		Object RDF = mapData.get(Constants.RDF_OBJECT);
		Object method = mapData.get(Constants.METHOD_ORIGIN);
		Object validationRDF = mapData.get(Constants.RDF_VALIDATION_MAPPER_OBJECT);
		if (RDF == null) {
			throw new MiddlewareHaltException(RDF_DATA_IS_MISSING);
		} else if (validationRDF == null) {
			throw new MiddlewareHaltException(RDF_VALIDATION_MAPPING_MISSING);
		} else if (!(RDF instanceof Model)) {
			throw new MiddlewareHaltException(RDF_DATA_IS_INVALID);
		} else if (!(validationRDF instanceof Model)) {
			throw new MiddlewareHaltException(RDF_VALIDATION_MAPPING_IS_INVALID);
		}else if (method == null){
			throw new MiddlewareHaltException(INVALID_REQUEST_PATH);
		}else if (schemaForCreate == null || schemaForUpdate == null) {
			throw new MiddlewareHaltException(SCHEMA_IS_NULL);
		} else {
			Schema schema = null;
			mergeModels((Model) RDF, (Model) validationRDF);
			ValidationResponse validationResponse = null;
			if(ADD_REQUEST_PATH.equals((String)method)){
				schema = schemaForCreate;
			} else {
				schema = schemaForUpdate;
			}
			Validator validator = new ShaclexValidator(schema, (Model) validationRDF);
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

}
