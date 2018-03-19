package io.opensaber.registry.schema.config;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import io.opensaber.registry.schema.config.SchemaConfigurator;


public class SchemaConfiguratorTest {
	
	private SchemaConfigurator schemaConfigurator;
	private String configFileSchema = "opensaber-schema-configuration.jsonld";
	
	private void initialize() throws IOException{
		schemaConfigurator = new SchemaConfigurator(configFileSchema);
	}

	@Test
	public void test_privacy_check_for_private_field() throws IOException{
		initialize();
		assertNotNull(schemaConfigurator.getSchemaConfig());
		assertTrue(schemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/birthDate"));
	}
	
	@Test
	public void test_privacy_check_for_non_private_field() throws IOException{
		initialize();
		assertNotNull(schemaConfigurator.getSchemaConfig());
		assertFalse(schemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/teacherName"));
	}

}
