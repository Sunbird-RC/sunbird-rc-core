package io.opensaber.validators.shex.shaclex;

import io.opensaber.pojos.ValidationResponse;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ShaclexValidatorTest {
	
	public static final String SCHEMAFORMAT = "SHEXC";
	public static final String PROCESSOR 	= "shex";

	@Test
	public void testValidateModelSchema() throws IOException, URISyntaxException {
		ShaclexValidator validator = new ShaclexValidator();
		String dataString = new String(Files.readAllBytes(Paths.get(getPath("good1.jsonld"))), StandardCharsets.UTF_8);	
		
		ValidationResponse validationResponse = validator.validate(dataString, "JSON-LD",Paths.get(getPath("good1.shex")).toString(), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(validationResponse);
		assertTrue(validationResponse.isValid());
	}

	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

}
