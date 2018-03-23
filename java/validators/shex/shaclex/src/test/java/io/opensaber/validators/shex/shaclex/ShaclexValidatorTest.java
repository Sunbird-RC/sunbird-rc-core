package io.opensaber.validators.shex.shaclex;

import es.weso.schema.Schema;
import es.weso.schema.Schemas;
import io.opensaber.pojos.ValidationResponse;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.junit.Test;
import scala.Option;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ShaclexValidatorTest {
	
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";

	@Test
	public void testValidateModelSchema() throws Exception {

		String dataString = new String(Files.readAllBytes(Paths.get(getPath("good1.jsonld"))), StandardCharsets.UTF_8);
		ValidationResponse validationResponse =
				validate(dataString, "JSON-LD",
						Paths.get(getPath("good1.shex")).toString(), SCHEMAFORMAT, PROCESSOR);
		assertNotNull(validationResponse);
		assertTrue(validationResponse.isValid());
	}

	private ValidationResponse validate(String data, String dataFormat, String schemaFile,
									   String schemaFormat, String processor) throws Exception {
		ShaclexValidator validator = new ShaclexValidator();
		System.out.println("Reading data JSONLD " + data);
		Model dataModel = ShaclexValidator.parse(data, dataFormat);//RDFDataMgr.loadModel(dataFile);
		Model validationRDF = generateShapeModel(dataModel);
		mergeModels(dataModel, validationRDF);
		System.out.println("Data model with the target shapes for validation: \n" + printRDF(validationRDF));
		Schema schema = readSchema(Paths.get(schemaFile), schemaFormat, processor);
		return validator.validate(validationRDF, schema);
	}

	private String printRDF(Model validationRdf) {
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, validationRdf, Lang.TTL);
		return sw.toString();
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

}
