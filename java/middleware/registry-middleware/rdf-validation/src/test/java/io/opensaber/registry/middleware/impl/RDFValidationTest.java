package io.opensaber.registry.middleware.impl;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import es.weso.schema.Schema;
import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.BaseMiddleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;

public class RDFValidationTest {
	private static final String SIMPLE_SHEX = "good1.shex";
	private static final String SIMPLE_JSONLD = "good1.jsonld";
	private static final String COMPLEX_TTL = "teacher.record";
	private static final String COMPLEX_INVALID_JSONLD = "teacher_badrecord.jsonld";
	private static final String COMPLEX_SHEX = "teacher.shex";
	private static final String SCHOOL_JSONLD = "school.jsonld";
	//public static final String FORMAT = "JSON-LD";
	public static final String TTL_FORMAT = "TTL";
	public static final String JSONLD_FORMAT = "JSONLD";
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";
	private String jsonld;
	private static final String EMPTY_STRING = "";
	private Map<String, Object> mapData;
	private BaseMiddleware middleware;
	
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private boolean setup(String shexFile) {
		boolean successfulInitialization = true;
		try {
			ShaclexValidator validator = new ShaclexValidator();
			Schema schema = validator.readSchema(shexFile, SCHEMAFORMAT, PROCESSOR);
			middleware = new RDFValidator(schema);
		} catch (Exception e) {
			successfulInitialization = false;
		}
		return successfulInitialization;
	}
	
	private boolean setup(Schema schema) {
		boolean successfulInitialization = true;
		try {
			middleware = new RDFValidator(schema);
		} catch (Exception e) {
			successfulInitialization = false;
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
		expectedEx.expectMessage("Data validation failed!");
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
		expectedEx.expectMessage("RDF validation mapping is missing!");
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		middleware.execute(mapData);
	}
	
	@Test
	public void testHaltIfValidationMappingIsNull() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF validation mapping is missing!");
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, null);
		middleware.execute(mapData);
	}
	
	@Test
	public void testHaltIfValidationMappingIsNotModel() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF validation mapping is invalid!");
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, "{}");
		middleware.execute(mapData);
		testForSuccessfulResult();
	}
	
	@Test
	public void testHaltIfSchemaIsMissing() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("Schema for validation is missing");
		Schema schema = null;
		assertTrue(setup(schema));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		Model model = getModel();
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, model);
		middleware.execute(mapData);
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

	private Model generateShapeModel(Model inputRdf) throws Exception {
		Model model = ModelFactory.createDefaultModel();
		Map<String, String> typeValidationMap = new HashMap<>();
		typeValidationMap.put("http://example.com/voc/teacher/1.0.0/School", "http://example.com/voc/teacher/1.0.0/SchoolShape");
		for (Map.Entry<String, String> map : typeValidationMap.entrySet()) {
			String key = map.getKey();
			StmtIterator iter = filterStatement(null, RDF.type, key, inputRdf);
			String value = map.getValue();
			if (value == null) {
				throw new Exception("Validation missing for type");
			}
			while (iter.hasNext()) {
				Resource subjectResource = ResourceFactory.createResource(value);
				Property predicate = ResourceFactory.createProperty("http://www.w3.org/ns/shacl#targetNode");
				model.add(subjectResource, predicate, iter.next().getSubject());
			}
		}
		return model;
	}

	private StmtIterator filterStatement(String subject, Property predicate, String object, Model resultModel) {
		Resource subjectResource = subject != null ? ResourceFactory.createResource(subject) : null;
		RDFNode objectResource = object != null ? ResourceFactory.createResource(object) : null;
		StmtIterator iter = resultModel.listStatements(subjectResource, predicate, objectResource);
		return iter;
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
	public void testIfaRealValidationFails() throws Exception {
		assertTrue(setup(COMPLEX_SHEX));
		mapData = new HashMap<>();
		Model dataModel = getValidRdf(COMPLEX_INVALID_JSONLD, JSONLD_FORMAT);
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_INVALID_JSONLD, JSONLD_FORMAT));
		Model model = generateShapeModel(dataModel);
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, model);
		middleware.execute(mapData);
		ValidationResponse response = (ValidationResponse) mapData.get(Constants.RDF_VALIDATION_OBJECT);
		assertFalse(response.isValid());
		Map<String, String> errorFields = response.getFields();
		errorFields.forEach((key, value) -> {
			assertEquals("http://example.com/voc/teacher/1.0.0/academicCalendarYearStart", key);
			assertEquals("2014 does not have datatype xsd:gYear", value);
		});
	}

	private void testForSuccessfulResult() {
		ValidationResponse validationResponse = testForResult();
		assertTrue(validationResponse.isValid());
	}

	private void testForUnsuccessfulResult() {
		ValidationResponse validationResponse = testForResult();
		assertFalse(validationResponse.isValid());
	}

	private ValidationResponse testForResult() {
		ValidationResponse validationResponse = (ValidationResponse)mapData.get(Constants.RDF_VALIDATION_OBJECT);
		assertNotNull(validationResponse);
		return validationResponse;
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

	private Model getValidRdf(String filename, String format) {
		setJsonld(filename);
		Model model = ShaclexValidator.parse(jsonld, format);
		return model;
	}
	
	private Model getValidRdf(String fileName){
		setJsonld(fileName);
		Model model = ShaclexValidator.parse(jsonld, TTL_FORMAT);
		return model;
	}
}
