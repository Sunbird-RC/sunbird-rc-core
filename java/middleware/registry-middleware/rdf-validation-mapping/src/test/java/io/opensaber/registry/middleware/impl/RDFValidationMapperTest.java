package io.opensaber.registry.middleware.impl;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import es.weso.schema.Schema;
import es.weso.schema.Schemas;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;
import scala.Option;
import scala.util.Either;

public class RDFValidationMapperTest {
	
	Map<String, Object> mapData;
	private Middleware m;
	private String jsonld;
	private Schema schema;
	private Model validationConfig;
	private static final String EMPTY_STRING = "";
	public static final String FORMAT = "JSON-LD";
	private static final String VALID_JSONLD = "school.jsonld";
	private static final String VALID_SHEX = "good1.shex";
	private static final String TARGET_NODE_IRI = "http://www.w3.org/ns/shacl#targetNode";
	private static final String SCHEMAFORMAT = "SHEXC";
	private static final String PROCESSOR 	= "shex";

	private Option<String> none = Option.empty();
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	private void setup(){
		m = new RDFValidationMapper(validationConfig);
	}
	
	public void loadSchemaForValidation(String validationFile) throws IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(validationFile);
		String contents = new String(ByteStreams.toByteArray(is));
		Either<String, Schema> result = Schemas.fromString(contents, SCHEMAFORMAT, PROCESSOR, none);
		schema = result.right().get();
	}
	
	public void loadValidationConfigModel(){
		validationConfig = RDFUtil.getRdfModelBasedOnFormat(schema.serialize(FORMAT).right().get(), FORMAT);
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

	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

	
	private Model getNewValidRdf(String fileName){
		setJsonld(fileName);
		Model model = RDFUtil.getRdfModelBasedOnFormat(jsonld, FORMAT);
		return model;
	}
	
	private Model getRdfWithDifferentType(String fileName){
		setJsonld(fileName);
		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(jsonld).getAsJsonObject();
		jsonObject.addProperty("@type", "School1");
		String dataString = new Gson().toJson(jsonObject);
		Model model = RDFUtil.getRdfModelBasedOnFormat(dataString, FORMAT);
		return model;
	}
	
	private Model getRdfWithComplexNodeRemoved(String fileName){
		setJsonld(fileName);
		JsonParser p = new JsonParser();
		JsonObject jsonObject = p.parse(jsonld).getAsJsonObject();
		jsonObject.remove("sample:address");
		String dataString = new Gson().toJson(jsonObject);
		Model model = RDFUtil.getRdfModelBasedOnFormat(dataString, FORMAT);
		return model;
	}
	
	private StmtIterator filterStatement(String subject, String predicate, Model resultModel){
		Resource subjectResource = subject!=null? ResourceFactory.createResource(subject) : null;
		Property predicateProp = predicate!=null? ResourceFactory.createProperty(predicate): null;
		StmtIterator iter = resultModel.listStatements(new SimpleSelector(subjectResource, predicateProp, (RDFNode) null));
		return iter;
	}

	@Test
	public void test_halt_if_no_rdf_to_map() throws IOException,MiddlewareHaltException{
		loadSchemaForValidation(VALID_SHEX);
		loadValidationConfigModel();
		setup();
		mapData = new HashMap<String,Object>();
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is missing");
		m.execute(mapData);
	}
	
	
	@Test
	public void test_halt_if_no_type_validation_map() throws IOException,MiddlewareHaltException{
		setup();
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, ModelFactory.createDefaultModel());
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("Validation is missing");
		m.execute(mapData);
	}
	
	@Test
	public void test_halt_configuration_present_invalid_rdf() throws IOException,MiddlewareHaltException{
		loadSchemaForValidation(VALID_SHEX);
		loadValidationConfigModel();
		setup();
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, "{}");
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF Data is invalid");
		m.execute(mapData);
	}
	
	@Test
	public void test_halt_configuration_present_shape_mapping_not_present() throws IOException,MiddlewareHaltException{
		loadSchemaForValidation(VALID_SHEX);
		loadValidationConfigModel();
		setup();
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getRdfWithDifferentType(VALID_JSONLD));
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("Validation missing for type");
		m.execute(mapData);
	}

	
	@Test
	public void test_valid_configuration_present_valid_rdf_present() throws IOException,MiddlewareHaltException{
		loadSchemaForValidation(VALID_SHEX);
		loadValidationConfigModel();
		setup();
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getNewValidRdf(VALID_JSONLD));
		mapData = m.execute(mapData);
		Model resultModel = (Model)mapData.get(Constants.RDF_VALIDATION_MAPPER_OBJECT);
		StmtIterator iter = filterStatement("http://example.com/voc/teacher/1.0.0/SchoolShape", TARGET_NODE_IRI, resultModel);
		assertTrue(iter.toList().size() == 1);
		
	}
	
	@Test
	public void test_valid_configuration_with_more_types_than_rdf() throws IOException,MiddlewareHaltException{
		loadSchemaForValidation(VALID_SHEX);
		loadValidationConfigModel();
		setup();
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RDF_OBJECT, getRdfWithComplexNodeRemoved(VALID_JSONLD));
		mapData = m.execute(mapData);
		Model resultModel = (Model)mapData.get(Constants.RDF_VALIDATION_MAPPER_OBJECT);
		StmtIterator iter1 = filterStatement("http://example.com/voc/teacher/1.0.0/SchoolShape", TARGET_NODE_IRI, resultModel);
		StmtIterator iter2 = filterStatement("http://example.com/voc/teacher/1.0.0/TeacherRoleShape", TARGET_NODE_IRI, resultModel);
		assertTrue(iter1.toList().size() == 1);
		assertTrue(iter2.toList().size() == 0);
	}

}


