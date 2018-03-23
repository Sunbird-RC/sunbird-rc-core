package io.opensaber.validators.shex.shaclex;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.weso.schema.*;
import es.weso.shapeMaps.ResultShapeMap;
import io.opensaber.pojos.ValidationInfo;
import io.opensaber.pojos.ValidationResponse;

import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.rdf.model.*;

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
	private Option<String> none = Option.empty();

	public ValidationResponse validate(Model dataModel, Schema schema) {
		RDFReader rdf = new RDFAsJenaModel(dataModel);
		Result result = schema.validate(rdf, "TARGETDECLS", null, none, none, rdf.getPrefixMap(), schema.pm());
		return parseValidationOutput(dataModel, result);
	}

	/**
	 * Method to parse the Validation error message from the Shaclex library to find out which field failed
	 * the shex validation
	 * @param dataModel
	 * @param result
	 * @return
	 */
	private ValidationResponse parseValidationOutput(Model dataModel, Result result) {

		Gson gson = new Gson();
		ValidationResponse validationResponse = new ValidationResponse("VALIDATION_ERROR");
		// Set the default value to true
		validationResponse.setValid(true);

		if (result.isValid()) {
			logger.info("Result is valid but may have non-conformant fields");
			logger.info("Valid. Result: " + result.toJsonString2spaces());

			System.out.println("Result is valid but may have non-conformant fields");
			System.out.println("Valid. Result: " + result.toJsonString2spaces());

			List<ResultShapeMap> validationOutput = JavaConverters.seqAsJavaList(result.shapeMaps());

			validationOutput.forEach((solution) -> {

				// logger.info("Result Shapemap:" + solution.toJson().toString());
				// System.out.println("Result Shapemap:" + solution.toJson().toString());
				Type collectionType = new TypeToken<Collection<ValidationInfo>>() {
				}.getType();
				Collection<ValidationInfo> validationInfoList = gson.fromJson(solution.toJson().toString(), collectionType);

				if (validationInfoList != null) {

					HashMap<String, String> errorList = new HashMap<>();

					validationInfoList.forEach((validationInfo) -> {

						if (validationInfo.getStatus().equalsIgnoreCase(NON_CONFORMANT)) {

							logger.info("Result Shapemap:" + solution.toJson().toString());

							validationResponse.setValid(false);
							validationResponse.setError("Data validation failed.");

							logger.info(String.format("Node Id: %s is NON-CONFORMANT with reason: %s"
									, validationInfo.getNode(), validationInfo.getReason()));
							System.out.println(String.format("Node Id: %s is NON-CONFORMANT with reason: %s"
									, validationInfo.getNode(), validationInfo.getReason()));
							String errorNode = validationInfo.getNode().replace("_:", "");

							/*
							 * The Shaclex library error message has three parts split by a \n newline character
							 * The last part of the message has a readable error message. The second last part has
							 * mapping between the node and the shape which failed the validation. We will be
							 * extracting these two parts from the error message.
							 */
							String[] errorInfo = validationInfo.getReason().split("\n");
							String readableErrorMsg = errorInfo[errorInfo.length - 1];
							String nodeValueWithError = extractErrorNodeValue(validationInfo.getReason());
							StmtIterator iterator = dataModel.listStatements();

							/*
							 * Iterate through the RDF statements to find the Node Id, Node Value combination which
							 * failed the validation.
							 */
							while (iterator.hasNext()) {
								Statement stmt = iterator.next();
								if (stmt.getSubject().toString().equals(errorNode)) {
									RDFNode object = stmt.getObject();
									if (object.isLiteral() && object.asLiteral().getString().equals(nodeValueWithError)) {
										logger.info(String.format("Field %s failed with validation error: %s", stmt.getPredicate(), readableErrorMsg));
										System.out.println(String.format("Field %s failed with validation error: %s", stmt.getPredicate(), readableErrorMsg));
										errorList.put(stmt.getPredicate().toString(), readableErrorMsg);
									}
								}
							}
						}
					});
					validationResponse.setFields(errorList);
				}

			});

			/*
			if (validationResponse.getFields().isEmpty()) {
				validationResponse.setValid(true);
			} else {
				validationResponse.setValid(false);
				validationResponse.setError("Data validation failed.");
			}
			*/

		} else {
			logger.info("Not valid");
			List<ErrorInfo> errors = JavaConverters.seqAsJavaList(result.errors());
			errors.forEach((e) ->
					logger.info(e.show()));
		}

		return validationResponse;
	}

	/**
	 * The error message from the Shaclex library will have a pattern like
	 * "Errors: Error: Attempt: node: 06-12-1990, shape: teacher:isADate". We use a regex to parse
	 * the node value from the error message string so that we can find the node in the RDF statement list
	 * which has this node value
	 * @param errorMessage
	 * @return
	 */
	private String extractErrorNodeValue(String errorMessage) {
		Pattern pattern = Pattern.compile("Error: Attempt: node:([^,]*)");
		Matcher m = pattern.matcher(errorMessage);
		final StringBuilder sb = new StringBuilder();
		if (m.find()) {
			sb.append(m.group(1));
		}
		return sb.toString().trim();
	}
	
	public static Model parse(String rdfData, String format) {
        Model m = ModelFactory.createDefaultModel();
        StringReader reader = new StringReader(rdfData);
        m.read(reader, null, format);
        return m;
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
