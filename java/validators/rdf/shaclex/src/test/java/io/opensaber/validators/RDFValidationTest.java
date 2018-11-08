package io.opensaber.validators;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.rdf.model.Model;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import es.weso.schema.Schema;
import es.weso.schema.Schemas;
import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.validators.rdf.shex.ErrorConstants;
import io.opensaber.validators.rdf.shex.RdfValidationServiceImpl;
import scala.Option;
import scala.util.Either;

public class RDFValidationTest {
	public static final String TTL_FORMAT = "TTL";
	public static final String JSONLD_FORMAT = "JSONLD";
	private static final String SIMPLE_SHEX = "good1.shex";
	private static final String COMPLEX_TTL = "teacher.record";
	private static final String COMPLEX_INVALID_JSONLD_UPDATE = "teacher_badrecord_update.jsonld";
	private static final String COMPLEX_INVALID_JSONLD_ADD = "teacher_badrecord_create.jsonld";
	private static final String COMPLEX_CREATE_SHEX = "teacher_create.shex";
	private static final String COMPLEX_UPDATE_SHEX = "teacher_update.shex";
	private static final String EMPTY_STRING = "";
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	private String jsonld;
	private RdfValidationServiceImpl rdfValidationServiceImpl;
	private Option<String> none = Option.empty();

	private Schema loadSchemaForValidation(String validationFile) throws IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(validationFile);
		if (is == null) {
			throw new IOException(Constants.VALIDATION_CONFIGURATION_MISSING);
		}
		String contents = new String(ByteStreams.toByteArray(is));
		Either<String, Schema> result = Schemas.fromString(contents, "SHEXC", "shex", Option.empty());
		return result.right().get();
	}

	private boolean setup(String shexFileForCreate, String shexFileForUpdate) {
		boolean successfulInitialization = true;
		try {
			rdfValidationServiceImpl = new RdfValidationServiceImpl(loadSchemaForValidation(shexFileForCreate),
					loadSchemaForValidation(shexFileForUpdate));
		} catch (Exception e) {
			successfulInitialization = false;
		}
		return successfulInitialization;
	}

	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

	@Test
	public void testHaltIfNoRDFToValidate() throws IOException, ValidationException {
		Assert.assertTrue(setup(SIMPLE_SHEX, SIMPLE_SHEX));
		expectedEx.expect(ValidationException.class);
		expectedEx.expectMessage(ErrorConstants.RDF_DATA_IS_MISSING);
		rdfValidationServiceImpl.validateRDFWithSchema(null, null);
	}

	@Ignore("Looks not a valid test anymore")
	@Test
	public void testHaltIfSchemaIsMissing() throws IOException, ValidationException {
		expectedEx.expect(ValidationException.class);
		expectedEx.expectMessage(ErrorConstants.SCHEMA_IS_NULL);
		Assert.assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		rdfValidationServiceImpl.validateRDFWithSchema(getValidRdf(COMPLEX_TTL), Constants.CREATE_METHOD_ORIGIN);
	}

	@Test
	public void testHaltIfMethodOriginIsMissing() throws IOException, ValidationException {
		expectedEx.expect(ValidationException.class);
		expectedEx.expectMessage(ErrorConstants.INVALID_REQUEST_PATH);
		Assert.assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		rdfValidationServiceImpl.validateRDFWithSchema(getValidRdf(COMPLEX_TTL), null);
	}

	@Test
	public void testIfComplexJSONLDIsSupportedForAdd() throws ValidationException {
		Assert.assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		ValidationResponse validationResponse = rdfValidationServiceImpl.validateRDFWithSchema(getValidRdf(COMPLEX_TTL),
				Constants.CREATE_METHOD_ORIGIN);
		testForSuccessfulResult(validationResponse);
	}

	@Test
	public void testIfComplexJSONLDIsSupportedForUpdate() throws ValidationException {
		Assert.assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		ValidationResponse validationResponse = rdfValidationServiceImpl.validateRDFWithSchema(getValidRdf(COMPLEX_TTL),
				Constants.UPDATE_METHOD_ORIGIN);
		testForSuccessfulResult(validationResponse);
	}

	@Test
	public void testIfaRealValidationFailsForAdd() throws Exception {
		Assert.assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		ValidationResponse response = rdfValidationServiceImpl.validateRDFWithSchema(
				getValidRdf(COMPLEX_INVALID_JSONLD_ADD, JSONLD_FORMAT), Constants.CREATE_METHOD_ORIGIN);
		Assert.assertFalse(response.isValid());
	}

	@Test
	public void testIfaRealValidationFailsForUpdate() throws Exception {
		Assert.assertTrue(setup(COMPLEX_CREATE_SHEX, COMPLEX_UPDATE_SHEX));
		Model dataModel = getValidRdf(COMPLEX_INVALID_JSONLD_UPDATE, JSONLD_FORMAT);
		ValidationResponse response = rdfValidationServiceImpl.validateRDFWithSchema(dataModel,
				Constants.UPDATE_METHOD_ORIGIN);

		Assert.assertFalse(response.isValid());
		Map<String, String> errorFields = response.getFields();
		errorFields.forEach((key, value) -> {
			Assert.assertEquals("http://example.com/voc/teacher/1.0.0/academicCalendarYearStart", key);
			Assert.assertEquals("2014 does not have datatype xsd:gYear", value);
		});
	}

	private void testForSuccessfulResult(ValidationResponse validationResponse) {
		Assert.assertTrue(validationResponse.isValid());
	}

	private void setJsonld(String filename) {

		try {
			String file = Paths.get(getPath(filename)).toString();
			jsonld = readFromFile(file);
		} catch (Exception e) {
			jsonld = EMPTY_STRING;
		}

	}

	private String readFromFile(String file) throws IOException, FileNotFoundException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		StringBuilder sb = new StringBuilder();
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (Exception e) {
			return EMPTY_STRING;
		} finally {
			if (reader != null) {
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

	private Model getValidRdf(String fileName) {
		setJsonld(fileName);
		Model model = RDFUtil.getRdfModelBasedOnFormat(jsonld, TTL_FORMAT);
		return model;
	}

	public Schema readSchema(String schemaFileName, String format, String processor) throws IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(schemaFileName);
		String contents = new String(ByteStreams.toByteArray(is));
		Either<String, Schema> result = Schemas.fromString(contents, format, processor, none);
		return result.right().get();
	}

}
