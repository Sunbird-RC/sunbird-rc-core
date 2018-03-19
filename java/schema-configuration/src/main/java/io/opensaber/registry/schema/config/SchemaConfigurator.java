package io.opensaber.registry.schema.config;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;

public class SchemaConfigurator {
	
	private static final String FORMAT = "JSON-LD";
	private Model schemaConfig;
	
	public SchemaConfigurator(String schemaFile) throws IOException{
		loadConfigModel(schemaFile);
	}
	
	public void loadConfigModel(String schemaFile) throws IOException{
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(schemaFile);
		String contents = new String(ByteStreams.toByteArray(is));
		schemaConfig = ShaclexValidator.parse(contents, FORMAT);
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

	public Model getSchemaConfig() {
		return schemaConfig;
	}

}
