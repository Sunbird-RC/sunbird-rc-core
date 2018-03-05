package io.opensaber.validators.shex.shaclex;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import es.weso.schema.*;
import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConverters;
import scala.Option;

import es.weso.rdf.RDFReader;
import es.weso.rdf.jena.RDFAsJenaModel;

public class ShaclexValidator {

	private static Logger logger = LoggerFactory.getLogger(ShaclexValidator.class);

	Option<String> none = Option.apply(null);

	public Result validate(Model dataModel, Schema schema) {
		RDFReader rdf = new RDFAsJenaModel(dataModel);
		Result result = schema.validate(rdf,"TARGETDECLS",none,none, rdf.getPrefixMap(), schema.pm());
		if (result.isValid()) {
			logger.info("Result is valid");
			logger.info("Valid. Result: " + result.show());
			List<Solution> solutions = JavaConverters.seqAsJavaList(result.solutions());
			solutions.forEach((solution) ->
			logger.info(solution.show()));
		} else {
			logger.info("Not valid");
			List<ErrorInfo> errors = JavaConverters.seqAsJavaList(result.errors());
			errors.forEach((e) ->
			logger.info(e.show()));
		}
		return result;
	} 

	public Result validate(String data, String dataFormat, String schemaFile, String schemaFormat, String processor) throws IOException {
		logger.info("Reading data JSONLD " + data);
		Model dataModel = parse(data,dataFormat);//RDFDataMgr.loadModel(dataFile);
		logger.info("Model read. Size = " + dataModel.size());
		logger.info("dataModel: " + dataModel);
		logger.info("Reading shapes file " + schemaFile + " with format " + schemaFormat);
		Schema schema = readSchema(Paths.get(schemaFile),schemaFormat, processor);
		logger.info("Schema read" + schema.serialize(schemaFormat));
		return validate(dataModel,schema);
	}    
	
	public static Model parse(String rdfData, String format) {
        Model m = ModelFactory.createDefaultModel();
        StringReader reader = new StringReader(rdfData);
        m.read(reader, null, format);
        return m;
	}

	public Schema readSchema(Path schemaFilePath, String format, String processor) throws IOException {
		String contents = new String(Files.readAllBytes(schemaFilePath));
		return Schemas.fromString(contents,format,processor,none).get();
	}

	public Schema readSchema(String schemaFileName, String format, String processor) throws IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(schemaFileName);
		String contents = new String(ByteStreams.toByteArray(is));
		return Schemas.fromString(contents,format,processor,none).get();
	}

}
