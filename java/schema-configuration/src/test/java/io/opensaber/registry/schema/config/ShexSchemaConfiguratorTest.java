package io.opensaber.registry.schema.config;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

import org.junit.Test;

import io.opensaber.registry.schema.configurator.ShexSchemaConfigurator;

public class ShexSchemaConfiguratorTest {

	private static final String CONFIG_SCHEMA_FILE = "opensaber-schema-configuration.jsonld";
	private ShexSchemaConfigurator shexSchemaConfigurator;

	private void initialize(String schemaConfigFile) throws IOException {
		shexSchemaConfigurator = new ShexSchemaConfigurator(schemaConfigFile);
	}

	@Test
	public void testIsPrivate() throws IOException {
		initialize(CONFIG_SCHEMA_FILE);
		assertNotNull(shexSchemaConfigurator);
		assertTrue(shexSchemaConfigurator.isPrivate("nationalIdentifier"));
		assertFalse(shexSchemaConfigurator.isPrivate("teacherName"));
	}

	@Test
	public void testIsEncrypted() throws Exception {
		byte[] array = new byte[7];
		new Random().nextBytes(array);
		String randomString = new String(array, Charset.forName("UTF-8"));
		String encryptedProperty = "encryptedschoolName";
		initialize(CONFIG_SCHEMA_FILE);

		assertEquals(true, shexSchemaConfigurator.isEncrypted(encryptedProperty));
		assertEquals(false, shexSchemaConfigurator.isEncrypted(null));
		assertEquals(false, shexSchemaConfigurator.isEncrypted(randomString));
	}

}
