package io.opensaber.registry.middleware.impl;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.Ignore;
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
	private static final String COMPLEX_INVALID_TTL = "teacher.badrecord";
	private static final String COMPLEX_SHEX = "teacher.shex";
	private static final String SCHOOL_JSONLD = "school.jsonld";
	//public static final String FORMAT = "JSON-LD";
	public static final String TTL_FORMAT = "TTL";
	private String jsonld;
	private static final String EMPTY_STRING = "";
	private Map<String, Object> mapData;
	private BaseMiddleware middleware;
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private boolean setup(String shexFile) throws URISyntaxException {
		boolean successfulInitialization = false;
		try {
			middleware = new RDFValidator(shexFile);
			successfulInitialization = true;
		} catch (NullPointerException e) {
		}
		return successfulInitialization;
	}

	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}
    
	@Test
	public void testHaltIfNoRDFToValidate() throws IOException, MiddlewareHaltException, URISyntaxException{
		assertTrue(setup(SIMPLE_SHEX));
		mapData = new HashMap<>();
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is missing!");
		middleware.execute(mapData);
	}
	
	@Test
	public void testHaltIfRDFpresentButInvalid() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is invalid!");
		assertTrue(setup(SIMPLE_SHEX));
		mapData = new HashMap<>();
		mapData.put(Constants.RDF_OBJECT, "{}");
		Model model = getModel();
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, model);
		middleware.execute(mapData);
	}
	

	@Test
	public void testHaltIfValidationMappingMissing() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF validation mapping is null!");
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		middleware.execute(mapData);
	}
	
	@Test
	public void testHaltIfValidationMappingIsNull() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF validation mapping is null!");
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, null);
		middleware.execute(mapData);
	}
	
	@Test @Ignore
	public void testHaltIfValidationMappingIsNotModel() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF validation mapping is invalid");
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, "{}");
		middleware.execute(mapData);
		testForSuccessfulResult();
	}
	
	@Test
	public void testIfComplexJSONLDIsSupported() throws IOException, MiddlewareHaltException, URISyntaxException{
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		Model model = getModel();
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, model);
		middleware.execute(mapData);
		testForSuccessfulResult();
	}

	private Model getModel() {
		Model model = ModelFactory.createDefaultModel();
		Resource subject = ResourceFactory.createResource("http://example.com/voc/teacher/1.0.0/SchoolShape");
		Property predicate = ResourceFactory.createProperty("http://www.w3.org/ns/shacl#targetNode");
		RDFNode object = ResourceFactory.createResource("http://example.com/voc/teacher/1.0.0/1234");
		model.add(subject, predicate, object);
		return model;
	}

	@Test
	public void testIfaRealValidationFails() throws IOException, URISyntaxException, MiddlewareHaltException {
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is invalid!");
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_INVALID_TTL));
		Model model = getModel();
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, model);
		try {
			middleware.execute(mapData);
		} catch (MiddlewareHaltException e) {
			testForUnsuccessfulResult();
			throw(e);
		}

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
	
	private void setJsonld(String filename){

		try {
			String file = Paths.get(getPath(filename)).toString();
			jsonld = readFromFile(file);	
		} catch (Exception e) {
			jsonld = EMPTY_STRING;
		}

	}

	private String readFromFile(String file) throws IOException,FileNotFoundException{
		BufferedReader reader = new BufferedReader(new FileReader (file));
		StringBuilder sb = new StringBuilder();
		try{
			String line = null;
			while((line = reader.readLine()) !=null){
				sb.append(line);
			}
		}catch(Exception e){
			return EMPTY_STRING;
		}finally{
			if(reader!=null){
				reader.close();
			}
		}
		return sb.toString();
	}

	
	private Model getValidRdf(String fileName){
		setJsonld(fileName);
		Model model = ShaclexValidator.parse(jsonld, TTL_FORMAT);
		return model;
	}
}
