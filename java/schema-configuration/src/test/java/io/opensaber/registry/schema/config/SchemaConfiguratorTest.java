package io.opensaber.registry.schema.config;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

import org.junit.Test;

import io.opensaber.registry.schema.config.SchemaConfigurator;


public class SchemaConfiguratorTest {
	
	private SchemaConfigurator schemaConfigurator;
	private static final String CONFIG_SCHEMA_FILE = "opensaber-schema-configuration.jsonld";
	private static final String CONFIG_VALIDATION_FILE = "validations.shex";
	
	private void initialize(String file, String validationFile) throws IOException{
		schemaConfigurator = new SchemaConfigurator(file, validationFile, validationFile, "http://example.com/voc/opensaber/1.0.0/");
	}

	@Test
	public void test_privacy_check_for_private_field() throws IOException{
		initialize(CONFIG_SCHEMA_FILE, CONFIG_VALIDATION_FILE);
		assertNotNull(schemaConfigurator.getSchemaConfig());
		assertTrue(schemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/birthDate"));
	}
	
	@Test
	public void test_privacy_check_for_non_private_field() throws IOException{
		initialize(CONFIG_SCHEMA_FILE, CONFIG_VALIDATION_FILE);
		assertNotNull(schemaConfigurator.getSchemaConfig());
		assertFalse(schemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/teacherName"));
	}
	
	@Test
	public void test_privacy_check_for_non_existent_field() throws IOException{
		initialize(CONFIG_SCHEMA_FILE, CONFIG_VALIDATION_FILE);
		assertNotNull(schemaConfigurator.getSchemaConfig());
		assertFalse(schemaConfigurator.isPrivate("sample"));
	}
	
	@Test
	public void test_single_valued_property() throws IOException{
		initialize(CONFIG_SCHEMA_FILE, CONFIG_VALIDATION_FILE);
		assertNotNull(schemaConfigurator.getValidationConfig());
		assertTrue(schemaConfigurator.isSingleValued("http://example.com/voc/teacher/1.0.0/serialNum"));
	}
	
	@Test
	public void test_multi_valued_property() throws IOException{
		initialize(CONFIG_SCHEMA_FILE, CONFIG_VALIDATION_FILE);
		assertNotNull(schemaConfigurator.getValidationConfig());
		assertFalse(schemaConfigurator.isSingleValued("http://example.com/voc/teacher/1.0.0/mainSubjectsTaught"));
	}
	
	@Test
	public void test_non_configured_property() throws IOException{
		initialize(CONFIG_SCHEMA_FILE, CONFIG_VALIDATION_FILE);
		assertNotNull(schemaConfigurator.getValidationConfig());
		assertTrue(schemaConfigurator.isSingleValued("http://example.com/voc/teacher/1.0.0/predicate"));
	}
	
	@Test
	public void test_isEncrypted() throws Exception {
		byte[] array = new byte[7];
		new Random().nextBytes(array);
		String randomString = new String(array, Charset.forName("UTF-8"));
		String encryptedProperty= "encryptedschoolName";
		initialize(CONFIG_SCHEMA_FILE, CONFIG_VALIDATION_FILE);
		
		assertEquals(true,schemaConfigurator.isEncrypted(encryptedProperty));
		assertEquals(false,schemaConfigurator.isEncrypted(null));
		assertEquals(false,schemaConfigurator.isEncrypted(randomString));
	}


}
