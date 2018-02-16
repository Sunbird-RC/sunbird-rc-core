package io.opensaber.validators.shex.shaclex;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
	public void testValidateModelSchema() throws IOException, URISyntaxException {
		ShaclexValidator validator = new ShaclexValidator();
		String dataString = new String(Files.readAllBytes(Paths.get(getPath("good1.jsonld"))), StandardCharsets.UTF_8);	
		
		Result result = validator.validate(dataString, "JSON-LD",Paths.get(getPath("good1.shex")).toString(), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(result);
		assertTrue(result.isValid());
	}

	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

}
