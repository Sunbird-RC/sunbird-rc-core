package io.opensaber.registry.service;

import es.weso.schema.Schema;
import es.weso.schema.Schemas;
import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.exception.RDFValidationException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.rdf.model.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scala.Option;
import scala.util.Either;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.*;

public class RDFValidationTest {
	private static final String SIMPLE_SHEX = "good1.shex";
	private static final String COMPLEX_TTL = "teacher.record";
	private static final String COMPLEX_INVALID_JSONLD_UPDATE = "teacher_badrecord_update.jsonld";
	private static final String COMPLEX_INVALID_JSONLD_ADD = "teacher_badrecord_create.jsonld";
	private static final String COMPLEX_CREATE_SHEX = "teacher_create.shex";
	private static final String COMPLEX_UPDATE_SHEX = "teacher_update.shex";
	public static final String TTL_FORMAT = "TTL";
	public static final String JSONLD_FORMAT = "JSONLD";
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";
	private String jsonld;
	private static final String EMPTY_STRING = "";
	private RDFValidator rdfValidator;
	private Option<String> none = Option.empty();
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private boolean setup(String shexFileForCreate, String shexFileForUpdate) {
		boolean successfulInitialization = true;
		try {
			Schema createSchema = readSchema(shexFileForCreate, SCHEMAFORMAT, PROCESSOR);
			Schema updateSchema = readSchema(shexFileForUpdate, SCHEMAFORMAT, PROCESSOR);
			rdfValidator = new RDFValidator(createSchema,updateSchema);
		} catch (Exception e) {
			successfulInitialization = false;
		}
		return successfulInitialization;
	}
	
	private boolean setup( String shexFileForUpdate) {
		boolean successfulInitialization = true;
		try {
			Schema createSchema = readSchema(shexFileForUpdate, SCHEMAFORMAT, PROCESSOR);
			rdfValidator = new RDFValidator(null, createSchema);
		} catch (Exception e) {
			successfulInitialization = false;
		}
		return successfulInitialization;
	}

	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

	@Test
	public void testHaltIfNoRDFToValidate() throws IOException, RDFValidationException {
		assertTrue(setup(SIMPLE_SHEX, SIMPLE_SHEX));
		expectedEx.expect(RDFValidationException.class);
		expectedEx.expectMessage("RDF Data is missing!");
		rdfValidator.validateRDFWithSchema(null,null);
	}

	@Test
	public void testHaltIfSchemaIsMissing() throws IOException, RDFValidationException {
		expectedEx.expect(RDFValidationException.class);
		expectedEx.expectMessage("Schema for validation is missing");
		assertTrue(setup(COMPLEX_UPDATE_SHEX));
		rdfValidator.validateRDFWithSchema(getValidRdf(COMPLEX_TTL), Constants.CREATE_METHOD_ORIGIN);
	}

	@Test
	public void testHaltIfMethodOriginIsMissing() throws IOException, RDFValidationException {
		expectedEx.expect(RDFValidationException.class);
		expectedEx.expectMessage("Request URL is invalid");
		assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		rdfValidator.validateRDFWithSchema(getValidRdf(COMPLEX_TTL),null);
	}

	@Test
	public void testIfComplexJSONLDIsSupportedForAdd() throws RDFValidationException {
		assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		ValidationResponse validationResponse =  rdfValidator.validateRDFWithSchema(getValidRdf(COMPLEX_TTL),Constants.CREATE_METHOD_ORIGIN);
		testForSuccessfulResult(validationResponse);
	}

	@Test
	public void testIfComplexJSONLDIsSupportedForUpdate() throws RDFValidationException {
		assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		ValidationResponse validationResponse = rdfValidator.validateRDFWithSchema(getValidRdf(COMPLEX_TTL), Constants.UPDATE_METHOD_ORIGIN);
		testForSuccessfulResult(validationResponse);
	}

	@Test
	public void testIfaRealValidationFailsForAdd() throws Exception {
		assertTrue(setup(COMPLEX_CREATE_SHEX,COMPLEX_UPDATE_SHEX));
		ValidationResponse response = rdfValidator.validateRDFWithSchema(getValidRdf(COMPLEX_INVALID_JSONLD_ADD, JSONLD_FORMAT), Constants.CREATE_METHOD_ORIGIN);
		assertFalse(response.isValid());
	}

	@Test
	public void testIfaRealValidationFailsForUpdate() throws Exception {
		assertTrue(setup(COMPLEX_CREATE_SHEX,COMPLEX_UPDATE_SHEX));
		Model dataModel = getValidRdf(COMPLEX_INVALID_JSONLD_UPDATE, JSONLD_FORMAT);
		ValidationResponse response = rdfValidator.validateRDFWithSchema(dataModel, Constants.UPDATE_METHOD_ORIGIN);

		assertFalse(response.isValid());
		Map<String, String> errorFields = response.getFields();
		errorFields.forEach((key, value) -> {
			assertEquals("http://example.com/voc/teacher/1.0.0/academicCalendarYearStart", key);
			assertEquals("2014 does not have datatype xsd:gYear", value);
		});
	}

	private void testForSuccessfulResult(ValidationResponse validationResponse) {
		assertTrue(validationResponse.isValid());
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
