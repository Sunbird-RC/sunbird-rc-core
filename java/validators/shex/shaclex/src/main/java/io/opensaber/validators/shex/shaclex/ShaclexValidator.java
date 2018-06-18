package io.opensaber.validators.shex.shaclex;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import es.weso.rdf.PrefixMap;
import es.weso.schema.*;
import es.weso.shapeMaps.ResultShapeMap;
import io.opensaber.pojos.ValidationInfo;
import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.Validator;
import org.apache.commons.lang3.StringUtils;
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

public class ShaclexValidator implements Validator{

	private static Logger logger = LoggerFactory.getLogger(ShaclexValidator.class);

	private static final String NON_CONFORMANT = "nonconformant";
	private Option<String> none = Option.empty();
	private Model dataModel;
	private Schema schema;
	
	public ShaclexValidator(Schema schema, Model dataModel){
		this.schema = schema;
		this.dataModel = dataModel;
	}

	public ValidationResponse validate() {
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
							validationResponse.setValid(false);
							validationResponse.setError("Data validation failed!");
							HashMap<String, String> errorList = extractValidationNodeInfo(dataModel, validationInfo, pm);
							validationResponse.getFields().putAll(errorList);
						}
					}
				}

			}

		} else {
			logger.info("Not valid");
			List<ErrorInfo> errors = JavaConverters.seqAsJavaList(result.errors());
			errors.forEach((e) -> {
				logger.info(e.show());
			});
		}
		return validationResponse;
	}

	private HashMap<String, String> extractValidationNodeInfo(Model dataModel, ValidationInfo validationInfo, PrefixMap pm) {

		System.out.println(String.format("Node Id: %s is NON-CONFORMANT with reason: %s"
				, validationInfo.getNode(), validationInfo.getReason()));

		// Remove the _: from the root node id if it is a blank node. If it is a URI (in the case of an update),
		// remove the <> from the URI in the error result node element.
		String errorNode = validationInfo.getNode().replace("_:", "")
				.replace("<", "").replace(">", "");

		/*
		 * The Shaclex library error message has three parts split by a \n newline character
		 * The last part of the message has a readable error message. The second last part has
		 * mapping between the node and the shape which failed the validation. We will be
		 * extracting these two parts from the error message.
		 */
		String[] errorInfo = validationInfo.getReason().trim().split("\n");
		ShaclexErrorData errorData;
		logger.info("Size of String[] by splitting Error message by newline: " + errorInfo.length);

		if(errorInfo.length > 1) {
			String readableErrorMsg = errorInfo[errorInfo.length - 1];
			String nodeValueWithError = extractErrorNodeValue(validationInfo.getReason(), "Error: Attempt: node:([^,]*)");
			if (StringUtils.isEmpty(nodeValueWithError)) {
				nodeValueWithError = extractErrorNodeValue(validationInfo.getReason(), "Errors: Error: (.*?) does not belong to");
			}
			if (nodeValueWithError.contains(":")) {
				nodeValueWithError = replaceContextPrefixWithContextUri(nodeValueWithError, pm);
			}
			Optional<String> nodeDataType = getNullableOptionalString(extractErrorNodeValue(validationInfo.getReason(),
					Pattern.quote("Datatype[") + "(.*?)" + Pattern.quote("->")));
			Optional<String> iriNodeIdentifier = getNullableOptionalString(extractErrorNodeValue(validationInfo.getReason(),
					"^Error: Node (.*?) has no shape"));
			// Remove the _: generated by Apache Jena library for Blank Nodes
			Optional<String> iriNode = iriNodeIdentifier.map(s -> {
				if(s.contains("_:")) {
					return s.substring(2);
				} else {
					return s;
				}
			});
			errorData = new ShaclexErrorData(nodeValueWithError, readableErrorMsg, nodeDataType, iriNode);
		} else {
			String readableErrorMsg = validationInfo.getReason();
			String nodeValueWithError = extractErrorNodeValue(validationInfo.getReason(), "^Error: (?s)(.*) does not");

			// If the error node contains the domain prefix, it needs to be replaced with the appropriate domain context URI.
			// For e.g., if the error node contains teacher:InServiceTeacherTrainingFromDIET, it needs to be translated to
			// http://example.com/voc/teacher/1.0.0/InServiceTeacherTrainingFromDIET. The prefix man contains the prefix URI
			// as <http://example.com/voc/teacher/1.0.0/> and hence the angled brackets need to be pruned.
			if(nodeValueWithError.contains(":")) {
				/*
				String contextPrefix = nodeValueWithError.substring(0, nodeValueWithError.indexOf(":"));
				String contextUri = pm.getIRI(contextPrefix).get().toString()
						.replace("<", "").replace(">", "");
				String nodeValue = nodeValueWithError.substring(nodeValueWithError.indexOf(":") + 1);
				nodeValueWithError = contextUri + nodeValue;
				*/
				nodeValueWithError = replaceContextPrefixWithContextUri(nodeValueWithError, pm);
			}
			Optional<String> iriNode = Optional.empty();
			Optional<String> nodeDataType = Optional.empty();
			errorData = new ShaclexErrorData(nodeValueWithError, readableErrorMsg, nodeDataType, iriNode);
		}
		HashMap<String, String> errorList =
				new HashMap<>(extractFieldInfoFromRDF(dataModel, errorNode, errorData));
		return errorList;
	}

	private String replaceContextPrefixWithContextUri(String nodeValueWithError, PrefixMap pm) {
		String contextPrefix = nodeValueWithError.substring(0, nodeValueWithError.indexOf(":"));
		String contextUri = pm.getIRI(contextPrefix).get().toString()
				.replace("<", "").replace(">", "");
		String nodeValue = nodeValueWithError.substring(nodeValueWithError.indexOf(":") + 1);
		nodeValueWithError = contextUri + nodeValue;
		return nodeValueWithError;
	}

	/**
	 * This method iterates through the RDF facts to extract the field information for which validation failed.
	 * @param dataModel
	 * @param parentErrorNode
	 * @param errorData
	 * @return
	 */
	private HashMap<String, String> extractFieldInfoFromRDF(Model dataModel, String parentErrorNode, ShaclexErrorData errorData) {
		StmtIterator iterator = dataModel.listStatements();
		HashMap<String, String> errorList = new HashMap<>();

		logger.info("Parent error node: " + parentErrorNode);
		logger.info("Error node value: " + errorData.getNodeValueWithError());
		logger.info("IRI node under which error node is present: " + errorData.getIriNode());
		logger.info("Error node datatype, if present: " + errorData.getNodeDataType());

		/*
		 * Iterate through the RDF statements to find the Node Id, Node Value combination which
		 * failed the validation.
		 */
		while (iterator.hasNext()) {
			Statement stmt = iterator.next();
			if (errorData.getIriNode().isPresent() && stmt.getSubject().toString().equals(errorData.getIriNode().get())) {
				String nodeMatchString = errorData.getNodeDataType().isPresent() ?
						errorData.getNodeValueWithError() + "^^" + errorData.getNodeDataType().get()
						: errorData.getNodeValueWithError();
				if (stmt.getObject().toString().equals(nodeMatchString)) {
					logger.info(String.format("Field %s failed with validation error: %s", stmt.getPredicate(), errorData.getReadableErrorMessage()));
					errorList.put(stmt.getPredicate().toString(), errorData.getReadableErrorMessage());
					break;
				}

			} else {
				if (stmt.getSubject().toString().equals(parentErrorNode)) {
					RDFNode object = stmt.getObject();
					String nodeMatchString = errorData.getNodeDataType().isPresent() ?
							errorData.getNodeValueWithError() + "^^" + errorData.getNodeDataType().get()
							: errorData.getNodeValueWithError();
					if (object.toString().equals(nodeMatchString)) {
						logger.info(String.format("Field %s failed with validation error: %s", stmt.getPredicate(), errorData.getReadableErrorMessage()));
						errorList.put(stmt.getPredicate().toString(), errorData.getReadableErrorMessage());
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

	private Optional<String> getNullableOptionalString(String input) {
		return Optional.ofNullable(input).filter(s -> !s.isEmpty());
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
