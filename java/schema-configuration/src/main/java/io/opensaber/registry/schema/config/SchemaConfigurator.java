package io.opensaber.registry.schema.config;

import java.io.IOException;
import java.io.InputStream;
import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import es.weso.schema.Schema;
import es.weso.schema.Schemas;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import scala.Option;
import scala.util.Either;

public class SchemaConfigurator {
	
	
	private String registrySystemBase;
	
	private static Logger logger = LoggerFactory.getLogger(SchemaConfigurator.class);

	private static final String FORMAT = "JSON-LD";
	private Schema schemaForCreate;
	private Schema schemaForUpdate;
	private Model schemaConfig;
	private Model validationConfig;
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";

	private Option<String> none = Option.empty();

	public SchemaConfigurator(String schemaFile, String validationcreateFile, String validationUpdateFile, String registrySystemBase) throws IOException {

		this.registrySystemBase = registrySystemBase;
		loadSchemaConfigModel(schemaFile);
		loadSchemaForValidation(validationcreateFile, true);
		loadSchemaForValidation(validationUpdateFile, false);
		loadValidationConfigModel();
	}

	private void loadSchemaConfigModel(String schemaFile) throws IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(schemaFile);
		if (is == null) {
			throw new IOException(Constants.SCHEMA_CONFIGURATION_MISSING);
		}
		String contents = new String(ByteStreams.toByteArray(is));
		schemaConfig = RDFUtil.getRdfModelBasedOnFormat(contents, FORMAT);
	}

	private void loadSchemaForValidation(String validationFile, boolean isCreate) throws IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(validationFile);
		if (is == null) {
			throw new IOException(Constants.VALIDATION_CONFIGURATION_MISSING);
		}
		String contents = new String(ByteStreams.toByteArray(is));
		Either<String, Schema> result = Schemas.fromString(contents, SCHEMAFORMAT, PROCESSOR, none);
		if (result.isLeft()) {
			logger.info("Error from schema validation = " + result.left().get());
		}
		if (isCreate) {
			schemaForCreate = result.right().get();
		} else {
			schemaForUpdate = result.right().get();
		}
	}

	private void loadValidationConfigModel() {
		validationConfig = RDFUtil.getRdfModelBasedOnFormat(schemaForUpdate.serialize(FORMAT).right().get(), FORMAT);
	}

	public boolean isPrivate(String propertyName) {
		Property property = ResourceFactory.createProperty(registrySystemBase + Constants.PRIVACY_PROPERTY);
		RDFNode rdfNode = ResourceFactory.createResource(propertyName);
		StmtIterator iter = schemaConfig.listStatements(null, property, rdfNode);
		return iter.hasNext();
	}
	
	public NodeIterator getAllPrivateProperties() {
		Property property = ResourceFactory.createProperty(registrySystemBase + Constants.PRIVACY_PROPERTY);
		return schemaConfig.listObjectsOfProperty(property);
	}

	public boolean isEncrypted(String tailPropertyKey) {
		if (tailPropertyKey != null) {
			return tailPropertyKey.substring(0, Math.min(tailPropertyKey.length(), 9)).equalsIgnoreCase("encrypted");
		} else
			return false;
	}

	public boolean isSingleValued(String property) {
		logger.debug("Property being verified for single-valued, multi-valued:" + property);
		Property predicate = ResourceFactory.createProperty("http://shex.io/ns/shex#predicate");
		RDFNode rdfNode = ResourceFactory.createResource(property);
		ResIterator resIter = validationConfig.listSubjectsWithProperty(predicate, rdfNode);
		while (resIter.hasNext()) {
			Resource subject = resIter.next();
			Long minValue = getValueConstraint("http://shex.io/ns/shex#min", subject);
			Long maxValue = getValueConstraint("http://shex.io/ns/shex#max", subject);
			if (minValue == null || maxValue == null) {
				logger.debug("Single-valued");
				return true;
			}
			if (minValue > 1) {
				logger.debug("Multi-valued");
				return false;
			} else if (maxValue > 1) {
				logger.debug("Multi-valued");
				return false;
			} else {
				logger.debug("Single-valued");
				return true;
			}
		}
		logger.debug("Property not matching any condition:" + property);
		return true;
	}

	private Long getValueConstraint(String constraint, Resource subject) {
		Property predicate = ResourceFactory.createProperty(constraint);
		NodeIterator nodeIter = validationConfig.listObjectsOfProperty(subject, predicate);
		while (nodeIter.hasNext()) {
			RDFNode node = nodeIter.next();
			if (node.isLiteral()) {
				Literal literal = node.asLiteral();
				return literal.getLong();
			} else if (node.isURIResource()) {
				return 2L;
			}
		}
		return null;
	}
	
	public Model getSchemaConfig() {
		return schemaConfig;
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
