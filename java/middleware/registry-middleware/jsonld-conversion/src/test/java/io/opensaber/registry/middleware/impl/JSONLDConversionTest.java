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

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.opensaber.converters.JenaRDF4J;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import io.opensaber.validators.shex.shaclex.ShaclexValidator;

public class JSONLDConversionTest {
	
	private static final String TEACHER_JSONLD = "teacher.jsonld";
	private static final String FORMAT = "JSON-LD";
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	Map<String, Object> mapData;
	private Middleware m;
	private Model rdfModel;
	
	private void setup() throws IOException, URISyntaxException{
    		m = new JSONLDConverter();
    		String jsonLDData = Paths.get(getPath(TEACHER_JSONLD)).toString();
    		Path filePath = Paths.get(jsonLDData);
    		String jsonld = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
    		org.apache.jena.rdf.model.Model model = RDFUtil.getRdfModelBasedOnFormat(jsonld, FORMAT);
    		rdfModel = JenaRDF4J.asRDF4JModel(model);
    }
	
	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

	@Test
	public void testHaltIfInvalidRdf() throws MiddlewareHaltException, IOException, URISyntaxException{
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF data is invalid!");
		setup();
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RESPONSE_ATTRIBUTE, "{}");
		m.execute(mapData);
	}
	
	@Test
	public void testValidRdf() throws URISyntaxException, IOException, MiddlewareHaltException{
		setup();
		mapData = new HashMap<String,Object>();
		mapData.put(Constants.RESPONSE_ATTRIBUTE, rdfModel);
		m.execute(mapData);
	}

}
