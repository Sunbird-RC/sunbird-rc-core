package io.opensaber.registry.middleware.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
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
	 
    private boolean setup(String shexFile) {
    	String file;
    	boolean successfulInitialization = false;
    	try {
    		file = getPath(shexFile);
    		Path filePath = Paths.get(file);
    		m = new RDFValidation(filePath);
    		successfulInitialization = true;
		} catch (NullPointerException e) {}
    	return successfulInitialization;
    }

	private String getPath(String file) throws NullPointerException {
		return this.getClass().getResource("/"+file).getPath();
	}
    
	@Test
	public void testHaltIfNoRDFToValidate() throws IOException, MiddlewareHaltException{
		assertTrue(setup(SIMPLE_SHEX));
		mapData = new HashMap<String,Object>();
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is missing!");
		m.execute(mapData);
	}
	
	@Test
	public void testHaltIfRDFpresentButInvalid() throws IOException, MiddlewareHaltException{
		assertTrue(setup(SIMPLE_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put("RDF", "{}");
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is invalid!");
		m.execute(mapData);
	}
	
	@Test
	public void testIfJSONLDIsSupported() throws IOException, MiddlewareHaltException{
		assertTrue(setup(SIMPLE_SHEX));
		mapData = new HashMap<String,Object>();
		String jsonLDData = getPath(SIMPLE_JSONLD);
		Path filePath = Paths.get(jsonLDData);
		String RDF = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
		Model dataModel = ShaclexValidator.parse(RDF,"JSON-LD");
		mapData.put("RDF", dataModel);
		m.execute(mapData);
		testForSuccessfulResult();
	}
	
	@Test
	public void testHaltIfvalidRDFpresentButFailsSHEX() throws IOException, MiddlewareHaltException{
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<String,Object>();
		String data = getPath(SIMPLE_JSONLD);
		Path filePath = Paths.get(data);
		String RDF = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
		Model dataModel = ShaclexValidator.parse(RDF,"JSON-LD");
		mapData.put("RDF", dataModel);
		m.execute(mapData);
		testForUnsuccessfulResult();
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
		Result validationResult = (Result)mapData.get("validationResult");
		assertNotNull(validationResult);
		return validationResult;
	}
}
