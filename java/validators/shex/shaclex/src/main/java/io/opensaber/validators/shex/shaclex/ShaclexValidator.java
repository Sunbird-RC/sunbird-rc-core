package io.opensaber.validators.shex.shaclex;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import es.weso.schema.*;
import es.weso.shapeMaps.ResultShapeMap;
import io.opensaber.pojos.Request;
import io.opensaber.pojos.ValidationInfo;
import io.opensaber.pojos.ValidationResponse;

import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import scala.collection.JavaConverters;
import scala.util.Either;
import scala.Option;

import es.weso.rdf.RDFReader;
import es.weso.rdf.jena.RDFAsJenaModel;

public class ShaclexValidator {

	private static Logger logger = LoggerFactory.getLogger(ShaclexValidator.class);
	
	private static final String NON_CONFORMANT = "nonconformant";

	Option<String> none = Option.apply(null);

	public ValidationResponse validate(Model dataModel, Schema schema) {
		RDFReader rdf = new RDFAsJenaModel(dataModel);
		Gson gson = new Gson();
		ValidationResponse validationResponse = new ValidationResponse();
		Result result = schema.validate(rdf,"TARGETDECLS",null,none,none, rdf.getPrefixMap(), schema.pm());
		if (result.isValid()) {
			logger.info("Result is valid but may have non-conformant fields");
			logger.info("Valid. Result: " + result.toJsonString2spaces());
			List<ResultShapeMap> solutions = JavaConverters.seqAsJavaList(result.shapeMaps());
			solutions.forEach((solution) -> {
				logger.info("Result Shapemap:"+solution.toJson().toString());
				Type collectionType = new TypeToken<Collection<ValidationInfo>>(){}.getType();
				Collection<ValidationInfo> validationInfoList = gson.fromJson(solution.toJson().toString(), collectionType);
				//ValidationInfo[] validationInfoArray =	gson.fromJson(solution.toJson().toString(), ValidationInfo[].class);
				if(validationInfoList!=null){
					List<String> errorList = new ArrayList<String>();
					validationInfoList.forEach((validationInfo) -> {
						if(validationInfo.getStatus().equalsIgnoreCase(NON_CONFORMANT)){
							logger.info("NON-CONFORMANT:"+validationInfo.getReason());
							errorList.add(validationInfo.getReason());
						}
					});
					validationResponse.setError(errorList);
				}
				
			});
			if(validationResponse.getError()==null || validationResponse.getError().size()==0){
				validationResponse.setValid(true);
			}
			
		} else {
			logger.info("Not valid");
			List<ErrorInfo> errors = JavaConverters.seqAsJavaList(result.errors());
			errors.forEach((e) ->
			logger.info(e.show()));
		}
		return validationResponse;
	} 

	public ValidationResponse validate(String data, String dataFormat, String schemaFile, String schemaFormat, String processor) throws IOException {
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
		return Schemas.fromString(contents,format,processor,none).right().get();
	}

	public Schema readSchema(String schemaFileName, String format, String processor) throws IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(schemaFileName);
		String contents = new String(ByteStreams.toByteArray(is));
		Either<String, Schema> result = Schemas.fromString(contents,format,processor,none);
		if(result.isLeft()){
		logger.info("Error from schema validation = " + result.left().get());
		}
		return Schemas.fromString(contents,format,processor,none).right().get();
	}

}
