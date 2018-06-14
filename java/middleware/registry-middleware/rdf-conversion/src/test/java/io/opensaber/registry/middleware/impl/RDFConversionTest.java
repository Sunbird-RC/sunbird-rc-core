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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;

public class RDFConversionTest {
	
	private static final String SIMPLE_JSONLD = "good1.jsonld";
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	Map<String, Object> mapData;
	private Middleware m;
	
	private void setup() {
    		m = new RDFConverter();
    }
	
	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

	@Test
	public void testHaltIfNoJsonLDDataToValidate() throws IOException, MiddlewareHaltException{
		setup();
		mapData = new HashMap<String,Object>();
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("JSON-LD data is missing!");
		m.execute(mapData);
	}
	
	@Test
	public void testHaltIfJSONLDpresentButInvalid() throws IOException, MiddlewareHaltException{
		setup();
		mapData = new HashMap<String,Object>();
		Object object = new Object();
		mapData.put(Constants.ATTRIBUTE_NAME, object);
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("JSONLD data is invalid!");
		m.execute(mapData);
	}
	
	@Test
	public void testIfJSONLDIsSupported() throws IOException, MiddlewareHaltException, URISyntaxException{
		setup();
		mapData = new HashMap<String,Object>();
		String jsonLDData = Paths.get(getPath(SIMPLE_JSONLD)).toString();
		Path filePath = Paths.get(jsonLDData);
		String jsonld = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
		mapData.put(Constants.ATTRIBUTE_NAME, jsonld);
		m.execute(mapData);
		testForSuccessfulResult();
	}
	
	private void testForSuccessfulResult() {
		Model resultModel = testForModel();
		assertFalse(resultModel.isEmpty());
	}

	private Model testForModel() {
		Model resultModel = (Model)mapData.get(Constants.RDF_OBJECT);
		assertNotNull(resultModel);
		return resultModel;
	}
}
