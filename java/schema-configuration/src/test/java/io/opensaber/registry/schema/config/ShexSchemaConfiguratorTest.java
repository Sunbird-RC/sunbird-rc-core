package io.opensaber.registry.schema.config;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import io.opensaber.registry.schema.configurator.ShexSchemaConfigurator;

public class ShexSchemaConfiguratorTest {

	private static final String CONFIG_SCHEMA_FILE = "opensaber-schema-configuration.jsonld";
	private static ShexSchemaConfigurator shexSchemaConfigurator;

	@BeforeClass
	public static void setup() throws IOException {
		shexSchemaConfigurator = new ShexSchemaConfigurator(CONFIG_SCHEMA_FILE);
	}

	@Test
	public void testIsPrivate() {
		assertNotNull(shexSchemaConfigurator);
		assertTrue(shexSchemaConfigurator.isPrivate("nationalIdentifier"));
		assertFalse(shexSchemaConfigurator.isPrivate("teacherName"));
	}

	@Test
	public void test_privacy_check_for_non_private_field() {
		assertFalse(shexSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/teacherName"));
	}

	@Test
	public void test_privacy_check_for_non_existent_field() {
		assertFalse(shexSchemaConfigurator.isPrivate("sample"));
	}

	@Test
	public void testIsEncrypted() {
		byte[] array = new byte[7];
		new Random().nextBytes(array);
		String randomString = new String(array, Charset.forName("UTF-8"));
		String encryptedProperty = "encryptedschoolName";

		assertEquals(true, shexSchemaConfigurator.isEncrypted(encryptedProperty));
		assertEquals(false, shexSchemaConfigurator.isEncrypted(null));
		assertEquals(false, shexSchemaConfigurator.isEncrypted(randomString));
	}

}
