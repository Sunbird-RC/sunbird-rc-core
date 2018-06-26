package io.opensaber.validators.shex.shaclex;

import com.google.gson.*;
import es.weso.schema.Schema;
import es.weso.schema.Schemas;
import io.opensaber.pojos.ValidationResponse;
import io.opensaber.registry.middleware.Validator;
import io.opensaber.registry.middleware.util.RDFUtil;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.junit.Test;
import scala.Option;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ShaclexValidatorTest {
	
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";

	@Test
	public void test_validate_model_schema() throws Exception {

		String dataString = new String(Files.readAllBytes(Paths.get(getPath("teacher.jsonld"))), StandardCharsets.UTF_8);
		ValidationResponse validationResponse =
				validate(dataString, "JSON-LD",
						Paths.get(getPath("teacher.shex")).toString(), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(validationResponse);
		assertTrue(validationResponse.isValid());
		assertEquals(0, validationResponse.getFields().size());
	}


	@Test
	public void test_validate_invalid_primitive_literal_datatype() throws Exception {

		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(new InputStreamReader
				(this.getClass().getClassLoader().getResourceAsStream("teacher.jsonld"))).getAsJsonObject();
		jsonObject.addProperty("serialNum", "14");

		String dataString = new Gson().toJson(jsonObject);
		ValidationResponse validationResponse =
				validate(dataString, "JSON-LD",
						Paths.get(getPath("teacher.shex")).toString(), SCHEMAFORMAT, PROCESSOR);

		assertNotNull(validationResponse);
		assertFalse(validationResponse.isValid());
		assertEquals(1, validationResponse.getFields().size());
		validationResponse.getFields().forEach((key, value) -> {
			assertEquals("http://example.com/voc/teacher/1.0.0/serialNum", key);
			assertEquals("14 does not have datatype xsd:integer", value);
		});
	}

	@Test
	public void test_validate_invalid_typed_literal_datatype() throws Exception {

		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(new InputStreamReader
				(this.getClass().getClassLoader().getResourceAsStream("teacher.jsonld"))).getAsJsonObject();
		jsonObject.add("nationalIdentifier", p.parse("{\"@value\": \"abc\", \"@type\": \"xsd:decimal\"}"));

		String dataString = new Gson().toJson(jsonObject);
		ValidationResponse validationResponse =
				validate(dataString, "JSON-LD",
						Paths.get(getPath("teacher.shex")).toString(), SCHEMAFORMAT, PROCESSOR);

		assertNotNull(validationResponse);
		assertFalse(validationResponse.isValid());
		assertEquals(1, validationResponse.getFields().size());
		validationResponse.getFields().forEach((key, value) -> {
			assertEquals("http://example.com/voc/teacher/1.0.0/nationalIdentifier", key);
			assertEquals("Details: Lexical form 'abc' is not a legal instance of " +
					"Datatype[http://www.w3.org/2001/XMLSchema#decimal -> class java.math.BigDecimal] Lexical form 'abc' " +
					"is not a legal instance of Datatype[http://www.w3.org/2001/XMLSchema#decimal -> class java.math.BigDecimal] " +
					"during parse -org.apache.xerces.impl.dv.InvalidDatatypeValueException: cvc-datatype-valid.1.2.1: 'abc' " +
					"is not a valid value for 'decimal'.", value);
		});
	}

	@Test
	public void test_validate_invalid_date_datatype() throws Exception {

		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(new InputStreamReader
				(this.getClass().getClassLoader().getResourceAsStream("teacher.jsonld"))).getAsJsonObject();
		jsonObject.addProperty("birthDate", "1990-12-06");

		String dataString = new Gson().toJson(jsonObject);
		ValidationResponse validationResponse =
				validate(dataString, "JSON-LD",
						Paths.get(getPath("teacher.shex")).toString(), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(validationResponse);
		assertFalse(validationResponse.isValid());
		assertEquals(1, validationResponse.getFields().size());
		validationResponse.getFields().forEach((key, value) -> {
			assertEquals("http://example.com/voc/teacher/1.0.0/birthDate", key);
			assertEquals("1990-12-06 does not have datatype xsd:date", value);
		});
	}

	@Test
	public void test_validate_enumerated_iri_property() throws Exception {

		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(new InputStreamReader
				(this.getClass().getClassLoader().getResourceAsStream("teacher.jsonld"))).getAsJsonObject();
		jsonObject.add("gender", p.parse("{\"@id\" : \"teacher:GenderTypeCode-INVALID\"}"));

		String dataString = new Gson().toJson(jsonObject);
		ValidationResponse validationResponse =
				validate(dataString, "JSON-LD",
						Paths.get(getPath("teacher.shex")).toString(), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(validationResponse);
		assertFalse(validationResponse.isValid());
		assertEquals(1, validationResponse.getFields().size());
		validationResponse.getFields().forEach((key, value) -> {
			assertEquals("http://example.com/voc/teacher/1.0.0/gender", key);
			assertEquals("Error: teacher:GenderTypeCode-INVALID does not belong to " +
					"[<http://example.com/voc/teacher/1.0.0/GenderTypeCode-MALE>,<http://example.com/voc/teacher/1.0.0/GenderTypeCode-FEMALE>]", value);
		});
	}

	@Test
	public void test_validate_nested_literal_property() throws Exception {

		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(new InputStreamReader
				(this.getClass().getClassLoader().getResourceAsStream("teacher.jsonld"))).getAsJsonObject();
		jsonObject.add("inServiceTeacherTrainingFromOthers",
				p.parse("{\"@type\":\"InServiceTeacherTrainingFromOthers\",\"teacher:daysOfInServiceTeacherTraining\": {\"@value\": \"abc\",\"@type\": \"xsd:decimal\"}}"));

		String dataString = new Gson().toJson(jsonObject);
		ValidationResponse validationResponse =
				validate(dataString, "JSON-LD",
						Paths.get(getPath("teacher.shex")).toString(), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(validationResponse);
		assertFalse(validationResponse.isValid());
		assertEquals(1, validationResponse.getFields().size());

		validationResponse.getFields().forEach((key, value) -> {
			assertEquals("http://example.com/voc/teacher/1.0.0/daysOfInServiceTeacherTraining", key);
			assertEquals("Details: Lexical form 'abc' is not a legal instance of " +
					"Datatype[http://www.w3.org/2001/XMLSchema#decimal -> class java.math.BigDecimal] Lexical form 'abc' " +
					"is not a legal instance of Datatype[http://www.w3.org/2001/XMLSchema#decimal -> class java.math.BigDecimal] " +
					"during parse -org.apache.xerces.impl.dv.InvalidDatatypeValueException: cvc-datatype-valid.1.2.1: 'abc' " +
					"is not a valid value for 'decimal'.", value);
		});

	}
	
	@Test
	public void test_target_and_validate_nested_entity() throws Exception {

		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(new InputStreamReader
				(this.getClass().getClassLoader().getResourceAsStream("teacher.jsonld"))).getAsJsonObject();
		JsonObject newJsonObject = jsonObject.getAsJsonObject("inServiceTeacherTrainingFromBRC");
		newJsonObject.add("@context", jsonObject.get("@context"));

		String dataString = new Gson().toJson(newJsonObject);
		ValidationResponse validationResponse =
				validate(dataString, "JSON-LD",
						Paths.get(getPath("teacher.shex")).toString(), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(validationResponse);
		assertTrue(validationResponse.isValid());
		assertEquals(0, validationResponse.getFields().size());

	}

	@Test
	public void test_validate_enumerated_constants_property() throws Exception {

		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(new InputStreamReader
				(this.getClass().getClassLoader().getResourceAsStream("teacher.jsonld"))).getAsJsonObject();
		jsonObject.add("socialCategory",
				p.parse("{\"@id\":\"teacher:SocialCategoryTypeCode-INVALID\"}"));

		String dataString = new Gson().toJson(jsonObject);
		ValidationResponse validationResponse =
				validate(dataString, "JSON-LD",
						Paths.get(getPath("teacher.shex")).toString(), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(validationResponse);
		assertFalse(validationResponse.isValid());
		assertEquals(1, validationResponse.getFields().size());

		validationResponse.getFields().forEach((key, value) -> {
			assertEquals("http://example.com/voc/teacher/1.0.0/socialCategory", key);
			assertEquals("Error: teacher:SocialCategoryTypeCode-INVALID does not belong to " +
					"[<http://example.com/voc/teacher/1.0.0/SocialCategoryTypeCode-GENERAL>," +
					"<http://example.com/voc/teacher/1.0.0/SocialCategoryTypeCode-SC>," +
					"<http://example.com/voc/teacher/1.0.0/SocialCategoryTypeCode-ST>," +
					"<http://example.com/voc/teacher/1.0.0/SocialCategoryTypeCode-OBC>," +
					"<http://example.com/voc/teacher/1.0.0/SocialCategoryTypeCode-ORC>," +
					"<http://example.com/voc/teacher/1.0.0/SocialCategoryTypeCode-OTHERS>]", value);
		});

	}

	@Test
	public void test_validate_nested_enumerated_constants_property() throws Exception {

		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(new InputStreamReader
				(this.getClass().getClassLoader().getResourceAsStream("teacher.jsonld"))).getAsJsonObject();

		jsonObject.add("basicProficiencyLevel",
				p.parse("[{\"@type\":\"BasicProficiencyLevel\",\"proficiencySubject\":{\"@id\":\"teacher:SubjectCode-SOCIALSTUDIES\"},\"proficiencyAcademicQualification\":{\"@id\":\"teacher:AcademicQualificationTypeCode-SOCIALSTUDIES\"}}]"));

		String dataString = new Gson().toJson(jsonObject);
		ValidationResponse validationResponse =
				validate(dataString, "JSON-LD",
						Paths.get(getPath("teacher.shex")).toString(), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(validationResponse);
		assertFalse(validationResponse.isValid());
		assertEquals(1, validationResponse.getFields().size());

		validationResponse.getFields().forEach((key, value) -> {
			assertEquals("http://example.com/voc/teacher/1.0.0/proficiencyAcademicQualification", key);
			assertEquals("Errors: Error: teacher:AcademicQualificationTypeCode-SOCIALSTUDIES does not belong to " +
					"[<http://example.com/voc/teacher/1.0.0/AcademicQualificationTypeCode-BELOWSECONDARY>," +
					"<http://example.com/voc/teacher/1.0.0/AcademicQualificationTypeCode-SECONDARY>," +
					"<http://example.com/voc/teacher/1.0.0/AcademicQualificationTypeCode-HIGHERSECONDARY>," +
					"<http://example.com/voc/teacher/1.0.0/AcademicQualificationTypeCode-GRADUATE>," +
					"<http://example.com/voc/teacher/1.0.0/AcademicQualificationTypeCode-POSTGRADUATE>," +
					"<http://example.com/voc/teacher/1.0.0/AcademicQualificationTypeCode-MPHIL>," +
					"<http://example.com/voc/teacher/1.0.0/AcademicQualificationTypeCode-POSTDOC>," +
					"<http://example.com/voc/teacher/1.0.0/AcademicQualificationTypeCode-PHD>]", value);
		});

	}

	private ValidationResponse validate(String data, String dataFormat, String schemaFile,
									   String schemaFormat, String processor) throws Exception {
		Model dataModel = RDFUtil.getRdfModelBasedOnFormat(data, dataFormat);
		Model validationRDF = generateShapeModel(dataModel);
		mergeModels(dataModel, validationRDF);
		Schema schema = readSchema(Paths.get(schemaFile), schemaFormat, processor);
		Validator validator = new ShaclexValidator(schema, validationRDF);
		return validator.validate();
	}


	private void mergeModels(Model RDF, Model validationRDF) {
		if (validationRDF != null) {
			validationRDF.add(RDF.listStatements());
		}
	}

	private Schema readSchema(Path schemaFilePath, String format, String processor) throws IOException {
		String contents = new String(Files.readAllBytes(schemaFilePath));
		return Schemas.fromString(contents, format, processor, Option.empty()).right().get();
	}

	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

	private Model generateShapeModel(Model inputRdf) throws Exception {
		Model model = ModelFactory.createDefaultModel();
		Map<String, String> typeValidationMap = new HashMap<>();
		typeValidationMap.put("http://example.com/voc/teacher/1.0.0/Teacher", "http://example.com/voc/teacher/1.0.0/TeacherShape");
		typeValidationMap.put("http://example.com/voc/teacher/1.0.0/InServiceTeacherTrainingFromBlockResourceCentre", "http://example.com/voc/teacher/1.0.0/InServiceTeacherTrainingShape");
		/*for (Map.Entry<String, String> map : typeValidationMap.entrySet()) {
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
		}*/
		List<Resource> labelNodes = getRootLabels(inputRdf);
		if(labelNodes.size()==1){
			Resource target = labelNodes.get(0);
			List<String> typeList = getTypeForSubject(inputRdf, target);
			if(typeList.size() == 1){
				String targetType = typeList.get(0);
				String shapeName = typeValidationMap.get(targetType);
				Resource subjectResource = ResourceFactory.createResource(shapeName);
				Property predicate = ResourceFactory.createProperty("http://www.w3.org/ns/shacl#targetNode");
				model.add(subjectResource,predicate, target);
			}
		}
		return model;
	}

	public static List<Resource> getRootLabels(Model rdfModel){
    	List<Resource> rootLabelList = new ArrayList<Resource>();
    	ResIterator resIter = rdfModel.listSubjects();
		while(resIter.hasNext()){
			Resource resource = resIter.next();
			StmtIterator stmtIter = rdfModel.listStatements(null, null, resource);
			if(!stmtIter.hasNext()){
				rootLabelList.add(resource);
			}
		}
		return rootLabelList;
    }
    
    public static List<String> getTypeForSubject(Model rdfModel, Resource root){
    	List<String> typeIRIs = new ArrayList<String>();
    	NodeIterator nodeIter = rdfModel.listObjectsOfProperty(root, RDF.type);
		while(nodeIter.hasNext()){
			RDFNode rdfNode = nodeIter.next();
			typeIRIs.add(rdfNode.toString());
		}
		return typeIRIs;
    }

}
