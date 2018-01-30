package io.opensaber.registry.middleware.impl;


import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.management.relation.RelationService;

import org.apache.jena.rdf.model.Model;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import es.weso.schema.Result;
import es.weso.schema.Schema;
import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;

/**
 * 
 * @author steotia
 *
 */
public class RDFValidator implements BaseMiddleware{

	private static final String RDF_DATA_IS_MISSING = "RDF Data is missing!";
	private Path schemaFilePath;	
	public static final String SCHEMAFORMAT = "SHEXC";
	public static final String PROCESSOR 	= "shex";
	
	public RDFValidator(Path schemaFilePath){
		this.schemaFilePath = schemaFilePath;
	}

	public Map<String, Object> execute(Map<String, Object> mapData) throws IOException, MiddlewareHaltException {
		Object RDF = mapData.get(Constants.RDF_OBJECT);
		if (RDF==null) {
			throw new MiddlewareHaltException(this.getClass().getName()+RDF_DATA_IS_MISSING); 
		} else if (RDF instanceof Model) {
			ShaclexValidator validator = new ShaclexValidator();
			Schema schema = validator.readSchema(this.schemaFilePath,SCHEMAFORMAT, PROCESSOR);
			mapData.put(Constants.RDF_VALIDATION_OBJECT, validator.validate((Model)RDF, schema));
		} else {
			throw new MiddlewareHaltException(this.getClass().getName()+"RDF Data is invalid!");
		}
		return null;
	}

	public Map<String, Object> next(Map<String, Object> mapData) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
