package io.opensaber.registry.middleware.impl;


import java.io.IOException;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import es.weso.schema.Schema;

import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;
import io.opensaber.pojos.ValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

public class RDFValidator implements BaseMiddleware{

	private static final String RDF_DATA_IS_MISSING = "RDF Data is missing!";
	private static final String RDF_DATA_IS_INVALID = "Data validation failed!";
	private static final String RDF_VALIDATION_MAPPING_IS_INVALID = "RDF validation mapping is invalid!";
	private static final String RDF_VALIDATION_MAPPING_MISSING = "RDF validation mapping is missing!";
    private static final String SCHEMA_IS_NULL = "Schema for validation is missing";
    private static final String SCHEMAFORMAT = "SHEXC";
    private static final String PROCESSOR 	= "shex";

    private static Logger prefLogger = LoggerFactory.getLogger("PERFORMANCE_INSTRUMENTATION");
	private String schemaFileName;

	StopWatch watch = new StopWatch();

	private Schema schema;
	
	public RDFValidator(Schema schema) {
		this.schema = schema;
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
		}else if (schema == null) {
			throw new MiddlewareHaltException(SCHEMA_IS_NULL);
		} else {
			ShaclexValidator validator = new ShaclexValidator();
			watch.start("RDF Validator: mergeModels() Performance Testing !");
			mergeModels((Model) RDF, (Model) validationRDF);
			watch.stop();
			prefLogger.info(watch.shortSummary());

			watch.start("RDF Validator: validate() Performance Testing !");
			ValidationResponse validationResponse = validator.validate((Model) validationRDF, schema);
			watch.stop();
			prefLogger.info(watch.shortSummary());

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
