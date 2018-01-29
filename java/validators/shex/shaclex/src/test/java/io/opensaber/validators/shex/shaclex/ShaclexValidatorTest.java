package io.opensaber.validators.shex.shaclex;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.scalactic.Validation;

import es.weso.schema.Result;
import es.weso.shacl.Path;

public class ShaclexValidatorTest {
	
	public static final String SCHEMAFORMAT = "SHEXC";
	public static final String PROCESSOR 	= "shex";

	@Test
	public void testValidateModelSchema() throws IOException {
		ShaclexValidator validator = new ShaclexValidator();
		String dataString = new String(Files.readAllBytes(Paths.get(getPath("good1.jsonld"))), StandardCharsets.UTF_8);		
		Result result = validator.validate(dataString, "JSON-LD",getPath("good1.shex"), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(result);
		assertTrue(result.isValid());
	}

	private String getPath(String file) {
		return this.getClass().getResource("/"+file).getPath();
	}

}
