package io.opensaber.validators.shex.shaclex;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.weso.rdf.PrefixMap;
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
		return parseValidationOutput(dataModel, result, schema.pm());
	}

	/**
	 * Method to parse the Validation error message from the Shaclex library to find out which field failed
	 * the shex validation
	 * @param dataModel
	 * @param result
	 * @return
	 */
	private ValidationResponse parseValidationOutput(Model dataModel, Result result, PrefixMap pm) {

		Gson gson = new Gson();
		ValidationResponse validationResponse = new ValidationResponse("VALIDATION_ERROR");

		// Set the default value to true
		validationResponse.setValid(true);
		validationResponse.setFields(new HashMap<>());

		if (result.isValid()) {
			logger.info("Result is valid but may have non-conformant fields");
			logger.info("Valid. Result: " + result.toJsonString2spaces());

			List<ResultShapeMap> validationOutput = JavaConverters.seqAsJavaList(result.shapeMaps());

			for(ResultShapeMap validationResult: validationOutput) {
				Type collectionType = new TypeToken<Collection<ValidationInfo>>() {
				}.getType();
				Collection<ValidationInfo> validationInfoList = gson.fromJson(validationResult.toJson().toString(), collectionType);

				if (validationInfoList != null) {
					for (ValidationInfo validationInfo : validationInfoList) {
						if (validationInfo.getStatus().equalsIgnoreCase(NON_CONFORMANT)) {
							logger.info("Result Shapemap: " + validationResult.toJson().toString());
							System.out.println("Result Shapemap: " + validationResult.toJson().toString());
							validationResponse.setValid(false);
							validationResponse.setError("Data validation failed.");
							HashMap<String, String> errorList = extractValidationNodeInfo(dataModel, validationInfo, pm);
							validationResponse.getFields().putAll(errorList);
						}
					}
				}

			}

		} else {
			logger.info("Not valid");
			System.out.println("Not valid");
			List<ErrorInfo> errors = JavaConverters.seqAsJavaList(result.errors());
			errors.forEach((e) -> {
				logger.info(e.show());
				System.out.println(e.show());
			});
		}
		return validationResponse;
	}

	private HashMap<String, String> extractValidationNodeInfo(Model dataModel, ValidationInfo validationInfo, PrefixMap pm) {

		logger.info(String.format("Node Id: %s is NON-CONFORMANT with reason: %s"
				, validationInfo.getNode(), validationInfo.getReason()));

		String errorNode = validationInfo.getNode().replace("_:", "");

		/*
		 * The Shaclex library error message has three parts split by a \n newline character
		 * The last part of the message has a readable error message. The second last part has
		 * mapping between the node and the shape which failed the validation. We will be
		 * extracting these two parts from the error message.
		 */
		String[] errorInfo = validationInfo.getReason().split("\n");
		String readableErrorMsg;
		String nodeValueWithError;
		Optional<String> iriNode = Optional.empty();
		Optional<String> nodeDataType = Optional.empty();

		logger.info("Error message split by new line character size " + errorInfo.length);

		if(errorInfo.length == 3) {
			readableErrorMsg = errorInfo[errorInfo.length - 1];
			nodeValueWithError = extractErrorNodeValue(validationInfo.getReason(), "Error: Attempt: node:([^,]*)");
		}
		else if(errorInfo.length == 4) {
			readableErrorMsg = errorInfo[errorInfo.length - 1];
			nodeDataType = Optional.of(extractErrorNodeValue(validationInfo.getReason(),
					Pattern.quote("Datatype[") + "(.*?)" + Pattern.quote("->")));
			nodeValueWithError = extractErrorNodeValue(validationInfo.getReason(), "Error: Attempt: node:([^,]*)");
		}
		/*
		 * The following condition is used to obtain the field name for a IRI resource with
		 * properties. The Shaclex library returns the node value with the schema prefix. We need to split
		 * the node value on the colon and then use the prefix map to get the context IRI
		 */
		else if(errorInfo.length == 5) {
			readableErrorMsg = errorInfo[errorInfo.length - 1];
			nodeDataType = Optional.of(extractErrorNodeValue(validationInfo.getReason(),
					Pattern.quote("Datatype[") + "(.*?)" + Pattern.quote("->")));
			iriNode = Optional.of(extractErrorNodeValue(validationInfo.getReason(),
					"^Error: Node (.*?) has no shape").substring(2));
			nodeValueWithError = extractErrorNodeValue(validationInfo.getReason(), "Error: Attempt: node:([^,]*)");
		}
		/*
		 * The following condition is used to obtain the field name for a IRI resource with enumerated values.
		 * The Shaclex library returns the node value with the schema prefix. We need to split
		 * the node value on the colon and then use the prefix map to get the context IRI
		 */
		else {
			nodeValueWithError = extractErrorNodeValue(validationInfo.getReason(), "^Error: (?s)(.*) does not");
			if(nodeValueWithError.contains(":")) {
				String contextPrefix = nodeValueWithError.substring(0, nodeValueWithError.indexOf(":"));
				String contextUri = pm.getIRI(contextPrefix).get().toString()
						.replace("<", "").replace(">", "");
				String nodeValue = nodeValueWithError.substring(nodeValueWithError.indexOf(":") + 1);
				nodeValueWithError = contextUri + nodeValue;
			}
			readableErrorMsg = validationInfo.getReason();
		}

		HashMap<String, String> errorList =
				new HashMap<>(extractFieldInfoFromRDF(dataModel, errorNode, nodeValueWithError, readableErrorMsg, iriNode, nodeDataType));
		return errorList;
	}

	/**
	 * This method iterates through the RDF facts to extract the field information for which validation failed.
	 * @param dataModel
	 * @param parentErrorNode
	 * @param nodeValueWithError
	 * @param readableErrorMsg
	 * @param iriErrorNode
	 * @return
	 */
	private HashMap<String, String> extractFieldInfoFromRDF(Model dataModel, String parentErrorNode, String nodeValueWithError,
															String readableErrorMsg, Optional<String> iriErrorNode, Optional<String> nodeDataType) {
		StmtIterator iterator = dataModel.listStatements();
		HashMap<String, String> errorList = new HashMap<>();

		/*
		 * Iterate through the RDF statements to find the Node Id, Node Value combination which
		 * failed the validation.
		 */
		while (iterator.hasNext()) {
			Statement stmt = iterator.next();
			if (iriErrorNode.isPresent() && stmt.getSubject().toString().equals(iriErrorNode.get())) {
				String nodeMatchString = nodeDataType.isPresent() ? nodeValueWithError + "^^" + nodeDataType.get() : nodeValueWithError;
				if (stmt.getObject().toString().equals(nodeMatchString)) {
					logger.info(String.format("Field %s failed with validation error: %s", stmt.getPredicate(), readableErrorMsg));
					errorList.put(stmt.getPredicate().toString(), readableErrorMsg);
					break;
				}

			} else {
				if (stmt.getSubject().toString().equals(parentErrorNode)) {
					RDFNode object = stmt.getObject();
					String nodeMatchString = nodeDataType.isPresent() ? nodeValueWithError + "^^" + nodeDataType.get() : nodeValueWithError;
					if (object.toString().equals(nodeMatchString)) {
						logger.info(String.format("Field %s failed with validation error: %s", stmt.getPredicate(), readableErrorMsg));
						errorList.put(stmt.getPredicate().toString(), readableErrorMsg);
						break;
					}
				}
			}
		}
		return errorList;
	}

	/**
	 * The error message from the Shaclex library will have a pattern like
	 * "Errors: Error: Attempt: node: 06-12-1990, shape: teacher:isADate". We use a regex to parse
	 * the node value from the error message string so that we can find the node in the RDF statement list
	 * which has this node value
	 * @param errorMessage
	 * @return
	 */
	private String extractErrorNodeValue(String errorMessage, String regex) {
		Pattern pattern = Pattern.compile(regex);
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
