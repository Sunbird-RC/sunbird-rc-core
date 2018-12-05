package io.opensaber.validators.rdf.shex;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import es.weso.schema.Schema;
import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.Validator;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.Constants.Direction;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.registry.transform.*;
import io.opensaber.validators.IValidate;
import io.opensaber.validators.ValidationException;

public class RdfValidationServiceImpl implements IValidate {

	private static final String SX_SHAPE_IRI = "http://shex.io/ns/shex#Shape";
	private static final String SHAPE_EXPRESSION_IRI = "http://shex.io/ns/shex#expression";
	private static final String SHAPE_EXPRESSIONS_IRI = "http://shex.io/ns/shex#expressions";
	private static final String SHAPE_VALUES_IRI = "http://shex.io/ns/shex#values";
	private static final String SHAPE_VALUE_EXPR_IRI = "http://shex.io/ns/shex#valueExpr";
	private static final String JSON_LD_FORMAT = "JSON-LD";
	@Autowired
	private RdfSignatureValidator signatureValidator;
	@Value("${signature.enabled}")
	private boolean signatureEnabled;
	private Map<String, String> shapeTypeMap;
	private Schema schemaForCreate;
	private Schema schemaForUpdate;
	
	@Autowired
	private APIMessage apiMessage;

	@Autowired
	private Transformer transformer;
	
	@Autowired
	private ConfigurationHelper configurationHelper;

	private RdfValidationServiceImpl() {
		// Disallow without schema
	}

	public RdfValidationServiceImpl(Schema createSchema, Schema updateSchema) {
		this.schemaForCreate = createSchema;
		this.schemaForUpdate = updateSchema;
		this.shapeTypeMap = getShapeMap(RDF.type, SX_SHAPE_IRI);
	}

	public Map<String, String> getShapeTypeMap() {
		return shapeTypeMap;
	}

	public ValidationResponse validateRDFWithSchema(Model rdf, String methodOrigin) throws ValidationException {
		Schema schema = null;
		if (null == rdf) {
			throw new ValidationException(ErrorConstants.RDF_DATA_IS_MISSING);
		}
		Model validationRdf = generateShapeModel(rdf);
		mergeModels(rdf, validationRdf);
		ValidationResponse validationResponse = null;
		if (Constants.CREATE_METHOD_ORIGIN.equals(methodOrigin)) {
			schema = schemaForCreate;
		} else if (Constants.UPDATE_METHOD_ORIGIN.equals(methodOrigin) || Constants.SEARCH_METHOD_ORIGIN.equals(methodOrigin)) {
			schema = schemaForUpdate;
		} else {
			throw new ValidationException(ErrorConstants.INVALID_REQUEST_PATH);
		}
		Validator validator = new ShaclexValidator(schema, validationRdf);
		validationResponse = validator.validate();
		return validationResponse;
	}

	public boolean validate(APIMessage apiMessage) throws MiddlewareHaltException {
		Model rdfModel = null;
		Object rdf = apiMessage.getLocalMap(Constants.RDF_OBJECT);
		String uri = apiMessage.getRequestWrapper().getRequestURI();
		String methodOrigin = uri.replace("/", "");

		String dataFromRequest = apiMessage.getRequest().getRequestMapAsString();
		String contentType = apiMessage.getRequestWrapper().getContentType();
		Data<Object> rdfData = null;

		try {
			Configuration config = configurationHelper.getConfiguration(contentType, Direction.IN);
			Data<Object> ldData = transformer.getInstance(config).transform(new Data<Object>(dataFromRequest));
			rdfData = transformer.getInstance(Configuration.LD2RDF).transform(ldData);

			apiMessage.addLocalMap(Constants.LD_OBJECT, ldData.getData().toString());
			apiMessage.addLocalMap(Constants.RDF_OBJECT, rdfData.getData());
			apiMessage.addLocalMap(Constants.CONTROLLER_INPUT, rdfData.getData());
			
			if (rdfData.getData() == null) {
				throw new ValidationException(ErrorConstants.RDF_DATA_IS_MISSING);
			} else if (!(rdfData.getData() instanceof Model)) {
				throw new ValidationException(ErrorConstants.RDF_DATA_IS_INVALID);
			} else if (methodOrigin == null) {
				throw new ValidationException(ErrorConstants.INVALID_REQUEST_PATH);
			} else if (schemaForCreate == null || schemaForUpdate == null) {
				throw new ValidationException(ErrorConstants.SCHEMA_IS_NULL);
			} else if (shapeTypeMap == null) {
				throw new ValidationException(this.getClass().getName() + ErrorConstants.VALIDATION_IS_MISSING);
			} else {
				rdfModel = (Model) rdfData.getData();
				ValidationResponse validationResponse = validateRDFWithSchema(rdfModel, methodOrigin);
				
				boolean result = validationResponse.isValid(); 
				if(signatureEnabled && (Constants.CREATE_METHOD_ORIGIN.equals(methodOrigin) || Constants.UPDATE_METHOD_ORIGIN.equals(methodOrigin))) {
						signatureValidator.validateMandatorySignatureFields(rdfModel);
				}
				return result;
			}
		}catch (TransformationException te){
			throw new MiddlewareHaltException(te.getMessage());
		} catch (ValidationException ve) {
			throw new MiddlewareHaltException(ve.getMessage());
		} catch (IOException ioe) {
			throw new MiddlewareHaltException(ioe.getMessage());
		}
	}

	private void mergeModels(Model RDF, Model validationRDF) {
		if (validationRDF != null) {
			validationRDF.add(RDF.listStatements());
		}
	}

	private Model generateShapeModel(Model inputRdf) throws ValidationException {
		Model model = ModelFactory.createDefaultModel();
		List<Resource> labelNodes = RDFUtil.getRootLabels(inputRdf);
		if (labelNodes.size() != 1) {
			throw new ValidationException(this.getClass().getName() + ErrorConstants.RDF_DATA_IS_INVALID);
		}

		Resource target = labelNodes.get(0);
		List<String> typeList = RDFUtil.getTypeForSubject(inputRdf, target);
		if (typeList.size() != 1) {
			throw new ValidationException(this.getClass().getName() + ErrorConstants.RDF_DATA_IS_INVALID);
		}
		String targetType = typeList.get(0);
		String shapeName = shapeTypeMap.get(targetType);
		if (shapeName == null) {
			throw new ValidationException(this.getClass().getName() + ErrorConstants.VALIDATION_MISSING_FOR_TYPE);
		}

		Resource subjectResource = ResourceFactory.createResource(shapeName);
		Property predicate = ResourceFactory.createProperty(Constants.TARGET_NODE_IRI);
		model.add(subjectResource, predicate, target);
		return model;
	}

	/**
	 * This method generates a shapemap which contains mappings between each entity
	 * type and the corresponding shape that the validations should target. Here we
	 * first filter out all the shape resources from the validationConfig. Then we
	 * iterate through the list of shape resources and do a bunch of filtering from
	 * the validationConfig based on a few predicates to finally arrive at the type
	 * for which the shape is targeted.
	 * 
	 * @param predicate
	 * @param object
	 * @param validationConfig
	 *            is the rdf model format of the Schema file used for validations
	 * @return
	 */
	private Map<String, String> getShapeMap(Property predicate, String object) {
		Map<String, String> shapeTypeMap = new HashMap<String, String>();
		Model validationConfig = getValidationConfigModel();
		List<Resource> shapeList = RDFUtil.getListOfSubjects(predicate, object, validationConfig);
		for (Resource shape : shapeList) {
			RDFNode node = getObjectAfterFilter(shape, SHAPE_EXPRESSION_IRI, validationConfig);
			RDFNode firstNode = getObjectAfterFilter(node, SHAPE_EXPRESSIONS_IRI, validationConfig);
			RDFNode secondNode = getObjectAfterFilter(firstNode, RDF.first.getURI(), validationConfig);
			RDFNode thirdNode = getObjectAfterFilter(secondNode, SHAPE_VALUES_IRI, validationConfig);
			if (thirdNode == null) {
				thirdNode = getObjectAfterFilter(secondNode, SHAPE_VALUE_EXPR_IRI, validationConfig);
			}
			RDFNode fourthNode = getObjectAfterFilter(thirdNode, SHAPE_VALUES_IRI, validationConfig);
			RDFNode typeNode = getObjectAfterFilter(fourthNode, RDF.first.getURI(), validationConfig);
			if (typeNode != null) {
				shapeTypeMap.put(typeNode.toString(), shape.toString());
				addOtherTypesForShape(fourthNode, validationConfig, shapeTypeMap, shape);
			}
		}
		return shapeTypeMap;
	}

	/**
	 * This method is created to include multiple types for a shape in the shapeMap.
	 * 
	 * @param subjectOfTypeNode
	 * @param validationConfig
	 * @param shapeMap
	 * @param shape
	 */
	private void addOtherTypesForShape(RDFNode subjectOfTypeNode, Model validationConfig,
			Map<String, String> shapeTypeMap, Resource shape) {
		RDFNode node = getObjectAfterFilter(subjectOfTypeNode, RDF.rest.getURI(), validationConfig);
		if (!node.equals(RDF.nil)) {
			RDFNode typeNode = getObjectAfterFilter(node, RDF.first.getURI(), validationConfig);
			shapeTypeMap.put(typeNode.toString(), shape.toString());
			addOtherTypesForShape(node, validationConfig, shapeTypeMap, shape);
		}
	}

	private RDFNode getObjectAfterFilter(RDFNode node, String predicate, Model validationConfig) {
		Property property = ResourceFactory.createProperty(predicate);
		List<RDFNode> nodeList = RDFUtil.getListOfObjectNodes((Resource) node, property, validationConfig);
		if (nodeList.size() != 0) {
			return nodeList.get(0);
		}
		return null;
	}

	private Model getValidationConfigModel() {
		return RDFUtil.getRdfModelBasedOnFormat(schemaForUpdate.serialize(JSON_LD_FORMAT).right().get(),
				JSON_LD_FORMAT);
	}

}
