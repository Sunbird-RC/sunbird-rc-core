package io.opensaber.validators.shex.shaclex;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import es.weso.schema.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.rdf.model.Model;
import scala.collection.JavaConverters;
import scala.Option;

import es.weso.rdf.RDFReader;
import es.weso.rdf.jena.RDFAsJenaModel;

public class ShaclexValidator {

	Option<String> none = Option.apply(null);

	public void validate(Model dataModel, Schema schema) throws Exception {
		RDFReader rdf = new RDFAsJenaModel(dataModel);
		Result result = schema.validate(rdf,"TARGETDECLS",none,none, rdf.getPrefixMap(), schema.pm());
		if (result.isValid()) {
			System.out.println("Result is valid");
			System.out.println("Valid. Result: " + result.show());
			List<Solution> solutions = JavaConverters.seqAsJavaList(result.solutions());
			solutions.forEach((solution) ->
			System.out.println(solution.show()));
		} else {
			System.out.println("Not valid");
			List<ErrorInfo> errors = JavaConverters.seqAsJavaList(result.errors());
			errors.forEach((e) ->
			System.out.println(e.show()));
		}
	} 

	public void validate(String dataFile, String schemaFile, String schemaFormat, String processor) throws Exception {
		System.out.println("Reading data file " + dataFile);
		Model dataModel = RDFDataMgr.loadModel(dataFile);
		System.out.println("Model read. Size = " + dataModel.size());
		System.out.println(dataModel);
		System.out.println("Reading shapes file " + schemaFile + " with format " + schemaFormat);
		Schema schema = readSchema(schemaFile,schemaFormat, processor);
		System.out.println("Schema read" + schema.serialize(schemaFormat));
		validate(dataModel,schema);
	}

	public Schema readSchema(String schemaFile, String format, String processor) throws Exception {
		String contents = new String(Files.readAllBytes(Paths.get(schemaFile)));
		return Schemas.fromString(contents,format,processor,none).get();
	}

}
