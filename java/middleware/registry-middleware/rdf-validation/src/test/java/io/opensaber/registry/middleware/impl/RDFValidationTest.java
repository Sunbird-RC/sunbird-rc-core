package io.opensaber.registry.middleware.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import es.weso.schema.Result;
import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;

public class RDFValidationTest {
	private static final String SIMPLE_SHEX = "good1.shex";
	private static final String SIMPLE_JSONLD = "good1.jsonld";
	private static final String COMPLEX_TTL = "teacher.record";
	private static final String COMPLEX_SHEX = "teacher.shex";
	Map<String, Object> mapData;
	private BaseMiddleware m;
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	 
    private boolean setup(String shexFile) throws URISyntaxException {
    	String file;
    	boolean successfulInitialization = false;
    	try {
    		file = Paths.get(getPath(shexFile)).toString();
    		Path filePath = Paths.get(file);
    		m = new RDFValidator(filePath);
    		successfulInitialization = true;
		} catch (NullPointerException e) {}
    	return successfulInitialization;
    }

	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}
    
	@Test
	public void testHaltIfNoRDFToValidate() throws IOException, MiddlewareHaltException, URISyntaxException{
		assertTrue(setup(SIMPLE_SHEX));
		mapData = new HashMap<String,Object>();
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is missing!");
		m.execute(mapData);
	}
	
	@Test
	public void testHaltIfRDFpresentButInvalid() throws IOException, MiddlewareHaltException, URISyntaxException{
		assertTrue(setup(SIMPLE_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, "{}");
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is invalid!");
		m.execute(mapData);
	}
	
	@Test
	public void testIfJSONLDIsSupported() throws IOException, MiddlewareHaltException, URISyntaxException{
		assertTrue(setup(SIMPLE_SHEX));
		mapData = new HashMap<String,Object>();
		String jsonLDData = Paths.get(getPath(SIMPLE_JSONLD)).toString();
		Path filePath = Paths.get(jsonLDData);
		String RDF = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
		Model dataModel = ShaclexValidator.parse(RDF,"JSON-LD");
		mapData.put(Constants.RDF_OBJECT, dataModel);
		m.execute(mapData);
		testForSuccessfulResult();
	}
	
	@Test
	public void testHaltIfvalidRDFpresentButFailsSHEX() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is invalid!");
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<String,Object>();
		String data = Paths.get(getPath(SIMPLE_JSONLD)).toString();
		Path filePath = Paths.get(data);
		String RDF = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
		Model dataModel = ShaclexValidator.parse(RDF,"JSON-LD");
		mapData.put(Constants.RDF_OBJECT, dataModel);
		m.execute(mapData);
//		testForUnsuccessfulResult();
	}

	private void testForSuccessfulResult() {
		Result validationResult = testForResult();
		assertTrue(validationResult.isValid());
	}

	private void testForUnsuccessfulResult() {
		Result validationResult = testForResult();
		assertFalse(validationResult.isValid());
	}

	private Result testForResult() {
		Result validationResult = (Result)mapData.get(Constants.RDF_VALIDATION_OBJECT);
		assertNotNull(validationResult);
		return validationResult;
	}
}
