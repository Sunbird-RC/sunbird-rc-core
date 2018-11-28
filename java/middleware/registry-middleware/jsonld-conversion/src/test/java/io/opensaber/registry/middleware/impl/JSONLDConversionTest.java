package io.opensaber.registry.middleware.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import io.opensaber.pojos.APIMessage;
import org.eclipse.rdf4j.model.Model;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.opensaber.converters.JenaRDF4J;
import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class JSONLDConversionTest {

	private static final String TEACHER_JSONLD = "teacher.jsonld";
	private static final String FORMAT = "JSON-LD";

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Mock
	public APIMessage apiMessage;

	Map<String, Object> mapData;
	private JSONLDConverter m;
	private Model rdfModel;

	private void setup() throws IOException, URISyntaxException {
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
	public void testHaltIfInvalidRdf() throws MiddlewareHaltException, IOException, URISyntaxException {
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("RDF data is invalid!");
		setup();
		mapData = new HashMap<String, Object>();
		mapData.put(Constants.RESPONSE_ATTRIBUTE, "{}");
		when(apiMessage.getLocalMap()).thenReturn(mapData);

		m.execute(apiMessage);
	}

	@Test
	public void testValidRdf() throws URISyntaxException, IOException, MiddlewareHaltException {
		setup();
		mapData = new HashMap<String, Object>();
		mapData.put(Constants.RESPONSE_ATTRIBUTE, rdfModel);

		when(apiMessage.getLocalMap()).thenReturn(mapData);

		m.execute(apiMessage);
	}

}
