package io.opensaber.registry.schema.config;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.rdf.model.Model;

import es.weso.schema.Schema;
import es.weso.schema.Schemas;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import scala.Option;
import scala.util.Either;

public class SchemaLoader {

	private Schema schemaForCreate;
	private Schema schemaForUpdate;
	private Model validationConfig;
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";
	private static final String JSON_LD = "JSON-LD";
	
	public SchemaLoader(String validationcreateFile, String validationUpdateFile) throws IOException{

		schemaForCreate = loadSchemaForValidation(validationcreateFile);
		schemaForUpdate = loadSchemaForValidation(validationUpdateFile);
		validationConfig = loadValidationConfigModel();
	}
	
	private Schema loadSchemaForValidation(String validationFile) throws IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(validationFile);
		if (is == null) {
			throw new IOException(Constants.VALIDATION_CONFIGURATION_MISSING);
		}
		String contents = new String(ByteStreams.toByteArray(is));
		Either<String, Schema> result = Schemas.fromString(contents, SCHEMAFORMAT, PROCESSOR, Option.empty());
		return result.right().get();
	}

	private Model loadValidationConfigModel() {
		return RDFUtil.getRdfModelBasedOnFormat(schemaForUpdate.serialize(JSON_LD).right().get(), JSON_LD);
	}
	
	public Model getValidationConfig(){
		return validationConfig;
	}

	public Schema getSchemaForCreate() {
		return schemaForCreate;
	}

	public Schema getSchemaForUpdate() {
		return schemaForUpdate;
	}

}
