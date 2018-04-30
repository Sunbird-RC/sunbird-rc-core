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

import es.weso.schema.Schema;
import es.weso.schema.Schemas;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;
import scala.Option;
import scala.util.Either;

public class SchemaConfigurator {
	
	private static Logger logger = LoggerFactory.getLogger(SchemaConfigurator.class);
	
	private static final String FORMAT = "JSON-LD";
	private Model schemaConfig;
	private Model validationConfig;
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";
	private Option<String> none = Option.empty();
	
	public SchemaConfigurator(String schemaFile, String validationFile) throws IOException{
		loadSchemaConfigModel(schemaFile);
		loadValidationConfigModel(validationFile);
	}
	
	public void loadSchemaConfigModel(String schemaFile) throws IOException{
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(schemaFile);
		String contents = new String(ByteStreams.toByteArray(is));
		schemaConfig = ShaclexValidator.parse(contents, FORMAT);
	}
	
	public void loadValidationConfigModel(String validationFile) throws IOException{
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(validationFile);
		String contents = new String(ByteStreams.toByteArray(is));
		Either<String, Schema> result = Schemas.fromString(contents,SCHEMAFORMAT,PROCESSOR,none);
		if(result.isLeft()){
		logger.info("Error from schema validation = " + result.left().get());
		}
		Schema schema = result.right().get();
		validationConfig = ShaclexValidator.parse(schema.serialize(FORMAT).right().get(),FORMAT);
	}
	
	public boolean isPrivate(String propertyName){
		Property property = ResourceFactory.createProperty(Constants.OPENSABER_CONTEXT_BASE+Constants.PRIVACY_PROPERTY);
		RDFNode rdfNode = ResourceFactory.createResource(propertyName);
		StmtIterator iter = schemaConfig.listStatements(null,property, rdfNode);
		while(iter.hasNext()){
			return true;
		}
		return false;
	}

	public boolean isEncrypted(String tailPropertyKey) {
		if (tailPropertyKey != null) {
			return tailPropertyKey.substring(0, Math.min(tailPropertyKey.length(), 9)).equalsIgnoreCase("encrypted");
		} else
			return false;
	}

	public boolean isSingleValued(String property){
		logger.info("Property being verified for single-valued, multi-valued:"+property);
    	Property predicate = ResourceFactory.createProperty("http://shex.io/ns/shex#predicate");
    	RDFNode rdfNode = ResourceFactory.createResource(property);
    	ResIterator resIter = validationConfig.listSubjectsWithProperty(predicate, rdfNode);
    	while(resIter.hasNext()){
    		Resource subject = resIter.next();
    		Long minValue = getValueConstraint("http://shex.io/ns/shex#min", subject);
    		Long maxValue = getValueConstraint("http://shex.io/ns/shex#max", subject);
    		if(minValue == null || maxValue == null){
    			logger.info("Single-valued");
    			return true;
    		}
    		if(minValue > 0){
    			logger.info("Multi-valued");
    			return false;
    		} else if(maxValue > 1){
    			logger.info("Multi-valued");
    			return false;
    		} else{
    			logger.info("Single-valued");
    			return true;
    		}
    	}
    	logger.info("Property not matching any condition:"+property);
    	return true;
    }
	
	private Long getValueConstraint(String constraint, Resource subject){
    	Property predicate = ResourceFactory.createProperty(constraint);
		NodeIterator nodeIter = validationConfig.listObjectsOfProperty(subject, predicate);
		while(nodeIter.hasNext()){
			RDFNode node = nodeIter.next();
			if(node.isLiteral()){
				Literal literal = node.asLiteral();
				Long value = literal.getLong();
				return value;
			} else if(node.isURIResource()){
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

}
