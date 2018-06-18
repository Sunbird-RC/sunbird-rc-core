package io.opensaber.registry.middleware.impl;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import es.weso.schema.Schema;
import es.weso.schema.Schemas;
import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.Validator;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;
import scala.Option;
import scala.util.Either;

public class RDFValidationTest {
	private static final String SIMPLE_SHEX = "good1.shex";
	private static final String SIMPLE_JSONLD = "good1.jsonld";
	private static final String COMPLEX_TTL = "teacher.record";
	private static final String COMPLEX_INVALID_JSONLD_UPDATE = "teacher_badrecord_update.jsonld";
	private static final String COMPLEX_INVALID_JSONLD_ADD = "teacher_badrecord_create.jsonld";
	private static final String COMPLEX_CREATE_SHEX = "teacher_create.shex";
	private static final String COMPLEX_UPDATE_SHEX = "teacher_update.shex";
	private static final String SCHOOL_JSONLD = "school.jsonld";
	private static final String ADD_REQUEST_PATH = "/add";
	private static final String UPDATE_REQUEST_PATH = "/update";
	//public static final String FORMAT = "JSON-LD";
	public static final String TTL_FORMAT = "TTL";
	public static final String JSONLD_FORMAT = "JSONLD";
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";
	private String jsonld;
	private static final String EMPTY_STRING = "";
	private Map<String, Object> mapData;
	private Middleware middleware;
	private Option<String> none = Option.empty();
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private boolean setup(String shexFileForCreate, String shexFileForUpdate) {
		boolean successfulInitialization = true;
		try {
			Schema createSchema = readSchema(shexFileForCreate, SCHEMAFORMAT, PROCESSOR);
			Schema updateSchema = readSchema(shexFileForUpdate, SCHEMAFORMAT, PROCESSOR);
			middleware = new RDFValidator(createSchema,updateSchema);
		} catch (Exception e) {
			successfulInitialization = false;
		}
		return successfulInitialization;
	}
	
	private boolean setup(Schema schema, String shexFileForUpdate) {
		boolean successfulInitialization = true;
		try {
			Schema createSchema = readSchema(shexFileForUpdate, SCHEMAFORMAT, PROCESSOR);
			middleware = new RDFValidator(null, createSchema);
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
		assertTrue(setup(SIMPLE_SHEX, SIMPLE_SHEX));
		mapData = new HashMap<>();
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is missing!");
		middleware.execute(mapData);
	}
	
	@Test
	public void testHaltIfRDFpresentButInvalid() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("Data validation failed!");
		assertTrue(setup(SIMPLE_SHEX, SIMPLE_SHEX));
		mapData = new HashMap<>();
		mapData.put(Constants.RDF_OBJECT, "{}");
		Model model = getModel();
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, model);
		middleware.execute(mapData);
	}
	

	/*@Test
	public void testHaltIfValidationMappingMissing() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF validation mapping is missing!");
		assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		middleware.execute(mapData);
	}
	
	@Test
	public void testHaltIfValidationMappingIsNull() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF validation mapping is missing!");
		assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, null);
		middleware.execute(mapData);
	}
	
	@Test
	public void testHaltIfValidationMappingIsNotModel() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF validation mapping is invalid!");
		assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, "{}");
		middleware.execute(mapData);
		testForSuccessfulResult();
	}*/
	
	@Test
	public void testHaltIfSchemaIsMissing() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("Schema for validation is missing");
		Schema schema = null;
		assertTrue(setup(schema,COMPLEX_UPDATE_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		mapData.put(Constants.METHOD_ORIGIN, ADD_REQUEST_PATH);
		Model model = getModel();
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, model);
		middleware.execute(mapData);
	}
	
	@Test
	public void testHaltIfMethodOriginIsMissing() throws IOException, MiddlewareHaltException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("Request URL is invalid");
		assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		Model model = getModel();
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, model);
		middleware.execute(mapData);
	}
	
	@Test
	public void testIfComplexJSONLDIsSupportedForAdd() throws IOException, MiddlewareHaltException, URISyntaxException{
		assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		mapData.put(Constants.METHOD_ORIGIN, ADD_REQUEST_PATH);
		Model model = getModel();
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, model);
		middleware.execute(mapData);
		testForSuccessfulResult();
	}
	
	@Test
	public void testIfComplexJSONLDIsSupportedForUpdate() throws IOException, MiddlewareHaltException, URISyntaxException{
		assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_TTL));
		mapData.put(Constants.METHOD_ORIGIN, UPDATE_REQUEST_PATH);
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
	public void testIfaRealValidationFailsForAdd() throws Exception {
		assertTrue(setup(COMPLEX_CREATE_SHEX,COMPLEX_UPDATE_SHEX));
		mapData = new HashMap<>();
		Model dataModel = getValidRdf(COMPLEX_INVALID_JSONLD_ADD, JSONLD_FORMAT);
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_INVALID_JSONLD_ADD, JSONLD_FORMAT));
		mapData.put(Constants.METHOD_ORIGIN, ADD_REQUEST_PATH);
		Model model = generateShapeModel(dataModel);
		mapData.put(Constants.RDF_VALIDATION_MAPPER_OBJECT, model);
		middleware.execute(mapData);
		ValidationResponse response = (ValidationResponse) mapData.get(Constants.RDF_VALIDATION_OBJECT);
		assertFalse(response.isValid());
	}

	@Test
	public void testIfaRealValidationFailsForUpdate() throws Exception {
		assertTrue(setup(COMPLEX_CREATE_SHEX,COMPLEX_UPDATE_SHEX));
		mapData = new HashMap<>();
		Model dataModel = getValidRdf(COMPLEX_INVALID_JSONLD_UPDATE, JSONLD_FORMAT);
		mapData.put(Constants.RDF_OBJECT, getValidRdf(COMPLEX_INVALID_JSONLD_UPDATE, JSONLD_FORMAT));
		mapData.put(Constants.METHOD_ORIGIN, UPDATE_REQUEST_PATH);
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
		Model model = RDFUtil.getRdfModelBasedOnFormat(jsonld, format);
		return model;
	}
	
	private Model getValidRdf(String fileName){
		setJsonld(fileName);
		Model model = RDFUtil.getRdfModelBasedOnFormat(jsonld, TTL_FORMAT);
		return model;
	}
	
	public Schema readSchema(String schemaFileName, String format, String processor) throws IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(schemaFileName);
		String contents = new String(ByteStreams.toByteArray(is));
		Either<String, Schema> result = Schemas.fromString(contents,format,processor,none);
		return result.right().get();
	}
	
}
