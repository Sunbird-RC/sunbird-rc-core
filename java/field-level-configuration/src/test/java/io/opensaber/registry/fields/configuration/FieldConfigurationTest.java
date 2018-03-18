package io.opensaber.registry.fields.configuration;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;


public class FieldConfigurationTest {
	
	private FieldConfiguration fieldConfiguration;
	private String configFileSchema = "teacher-privacy.jsonld";
	
	private void initialize() throws IOException{
		fieldConfiguration = new FieldConfiguration(configFileSchema);
	}

	@Test
	public void test_privacy_check_for_private_field() throws IOException{
		initialize();
		assertNotNull(fieldConfiguration.getConfigRdf());
		assertTrue(ConfigurationFilter.isExistingConfiguration("http://example.com/voc/teacher/1.0.0/privateFields",
				"http://example.com/voc/teacher/1.0.0/nationalIdentifier",fieldConfiguration.getConfigRdf()));
	}
	
	@Test
	public void test_privacy_check_for_non_private_field() throws IOException{
		initialize();
		assertNotNull(fieldConfiguration.getConfigRdf());
		assertFalse(ConfigurationFilter.isExistingConfiguration("http://example.com/voc/teacher/1.0.0/privateFields",
				"http://example.com/voc/teacher/1.0.0/teacherName",fieldConfiguration.getConfigRdf()));
	}

}
