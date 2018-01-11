package io.opensaber.validators.shex.shaclex;

import org.junit.Test;

public class ShaclexValidatorTest {
	
	public static final String SCHEMAFORMAT = "SHEXC";
	public static final String PROCESSOR 	= "shex";

	@Test
	public void testValidateModelSchema() throws Exception {
		ShaclexValidator validator = new ShaclexValidator();
		validator.validate(getPath("good1.ttl"), getPath("good1.shex"), SCHEMAFORMAT, PROCESSOR);
	}

	private String getPath(String file) {
		return this.getClass().getResource("/"+file).getPath();
	}

}
