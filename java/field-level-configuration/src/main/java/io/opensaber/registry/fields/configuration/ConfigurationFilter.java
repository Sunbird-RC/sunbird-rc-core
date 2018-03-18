package io.opensaber.registry.fields.configuration;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class ConfigurationFilter {
	
	public static boolean isExistingConfiguration(String predicate, String objectValue,Model configRdf){
		Property property = ResourceFactory.createProperty(predicate);
		RDFNode rdfNode = ResourceFactory.createResource(objectValue);
		StmtIterator iter = configRdf.listStatements(null,property, rdfNode);
		while(iter.hasNext()){
			return true;
		}
		return false;
	}

}