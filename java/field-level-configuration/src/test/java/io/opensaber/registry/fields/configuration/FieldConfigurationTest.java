package io.opensaber.registry.fields.configuration;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

public class FieldConfigurationTest {
	
	private FieldConfiguration fieldConfiguration;
	
	private void initialize(){
		fieldConfiguration = new FieldConfiguration();
	}

	@Test
	public void test_privacy_check_for_existing_field() {
		initialize();
		assertTrue(fieldConfiguration.getPrivacyForField("nationalIndentifier"));
	}
	
	@Test @Ignore
	public void test_privacy_check_for_non_existing_field() {
		initialize();
		assertFalse(fieldConfiguration.getPrivacyForField("teacherName"));
	}

}
