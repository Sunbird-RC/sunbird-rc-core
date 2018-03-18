package io.opensaber.registry.fields.configuration;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;

public class FieldConfiguration {
	
	@Autowired
	private Environment environment;
	
	private static final String FORMAT = "JSON-LD";
	private String configJsonldFile;
	private Model configRdf;
	
	public FieldConfiguration(String configJsonldFile) throws IOException{
		this.configJsonldFile = configJsonldFile;
		loadConfigModel();
	}
	
	public void loadConfigModel() throws IOException{
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(this.configJsonldFile);
		String contents = new String(ByteStreams.toByteArray(is));
		configRdf = ShaclexValidator.parse(contents, FORMAT);
	}
	
	public boolean getPrivacyForField(String fieldName){
		String registryContextBase = environment.getProperty(Constants.REGISTRY_CONTEXT_BASE);
		String privacyProperty = environment.getProperty(Constants.PRIVACY_PROPERTY);
		return ConfigurationFilter.isExistingConfiguration(registryContextBase+privacyProperty, fieldName, configRdf);
	}

	public Model getConfigRdf() {
		return configRdf;
	}

}
