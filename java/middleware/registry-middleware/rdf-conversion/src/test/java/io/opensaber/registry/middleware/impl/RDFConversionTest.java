package io.opensaber.registry.middleware.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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
import org.apache.jena.rdf.model.Model;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.opensaber.registry.middleware.Middleware;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.Assert;

@RunWith(MockitoJUnitRunner.class)
public class RDFConversionTest {

	private static final String SIMPLE_JSONLD = "good1.jsonld";

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Mock
	private APIMessage apiMessage;



	private Middleware m;

	private void setup() {
		m = new RDFConverter();
	}

	private URI getPath(String file) throws URISyntaxException {
		return this.getClass().getClassLoader().getResource(file).toURI();
	}

	@Test
	public void testHaltIfNoJsonLDDataToValidate() throws IOException, MiddlewareHaltException {
		setup();
		Map<String, Object> mapData = new HashMap<String, Object>();
		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage("JSON-LD data is missing!");
		m.execute(apiMessage);
	}

	@Test
	public void testHaltIfJSONLDpresentButInvalid() throws IOException, MiddlewareHaltException {
		setup();
		Object object = new Object();
		Map<String, Object> mapData = new HashMap<>();
		mapData.put(Constants.LD_OBJECT, object);

		when(apiMessage.getLocalMap()).thenReturn(mapData);

		expectedEx.expect(MiddlewareHaltException.class);
		expectedEx.expectMessage(Constants.JSONLD_PARSE_ERROR);
		m.execute(apiMessage);
	}

	@Test
	public void testIfJSONLDIsSupported() throws IOException, MiddlewareHaltException, URISyntaxException {
		setup();
		Map<String, Object> mapData = new HashMap<String, Object>();
		String jsonLDData = Paths.get(getPath(SIMPLE_JSONLD)).toString();
		Path filePath = Paths.get(jsonLDData);
		String jsonld = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);

		mapData.put(Constants.LD_OBJECT, jsonld);

		when(apiMessage.getLocalMap()).thenReturn(mapData);

		m.execute(apiMessage);
		assertTrue(mapData.containsKey(Constants.RDF_OBJECT));
	}

	private void testForSuccessfulResult() {
		Model resultModel = testForModel();
		assertFalse(resultModel.isEmpty());
	}

	private Model testForModel() {
		Map<String, Object> mapData = new HashMap<>();
		Model resultModel = (Model) mapData.get(Constants.RDF_OBJECT);
		assertNotNull(resultModel);
		return resultModel;
	}
}
