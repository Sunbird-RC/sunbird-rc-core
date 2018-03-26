package io.opensaber.registry.service.impl;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestGatewaySupport;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.dao.impl.RegistryDaoImpl;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.schema.config.SchemaConfigurator;
import io.opensaber.registry.service.EncryptionService;
import io.opensaber.registry.sink.DatabaseProvider;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { EncryptionServiceImpl.class, Environment.class, GenericConfiguration.class, })
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryEncryptionServiceImplTest extends RegistryTestBase {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Value("${encryption.uri}")
	private String encryptionUri;

	@Value("${decryption.uri}")
	private String decryptionUri;

	@Autowired
	private EncryptionService encryptionService;
	
	@Mock
	private SchemaConfigurator mockSchemaConfigurator;


	@Mock
	private RestTemplate restTemplate;
	
	private MockRestServiceServer mockServer;

	@Autowired
	private Environment environment;
	
	@Before
	public void setUp() {
	//try to use existing RestTemplte instead
	mockServer = MockRestServiceServer.createServer(restTemplate);
	}

	@Test
	public void test_encrypted_value_as_expected() throws Exception {
		String value = "1234567890123456";
		String encryptedValue = encryptionService.encrypt(value);
		assertNotEquals(value, encryptedValue);
		assertNotNull(encryptedValue);
	}

	@Test
	public void test_decrypted_value_as_expected() throws Exception {
		String value = "v1|11|PKCS1|eYIVlw6o/KVl9LhbW+WmQJO3WHU8pUaQa5lRpggBPs/l9TThFA5tNzx2nO0mSlP0sgauSGdR+zEdHDzgIFw2yA==";
		String decryptedValue = encryptionService.decrypt(value);
		assertEquals("1234567890123456", decryptedValue);
	}

	@Test
	public void test_isEncryptable() throws Exception {
		byte[] array = new byte[7];
		new Random().nextBytes(array);
		String randomString = new String(array, Charset.forName("UTF-8"));
		when(mockSchemaConfigurator.isPrivate("http://example.com/voc/teacher/1.0.0/schoolName")).thenReturn(true);	
		when(mockSchemaConfigurator.isPrivate(randomString)).thenReturn(false);	
		
		assertEquals(true,encryptionService.isEncryptable("http://example.com/voc/teacher/1.0.0/schoolName"));
		assertEquals(false,encryptionService.isEncryptable(null));
		assertEquals(false,encryptionService.isEncryptable(randomString));	
	}

	@Test
	public void test_isDecryptable() throws Exception {
		byte[] array = new byte[7];
		new Random().nextBytes(array);
		String randomString = new String(array, Charset.forName("UTF-8"));
		String encryptedProperty= "encryptedschoolName";
	    
		assertEquals(true,encryptionService.isDecryptable(encryptedProperty));
		assertEquals(false,encryptionService.isDecryptable(null));
		assertEquals(false,encryptionService.isDecryptable(randomString));
	}

	@Test
	public void test_service_encryption() throws Exception {
	
		 byte[] array = new byte[7];
		 new Random().nextBytes(array);
		 String generatedString = new String(array, Charset.forName("UTF-8"));
		 String response = encryptionService.encrypt(generatedString);
		 
		 assertNotEquals(generatedString,response);
		 assertNotEquals(null,response);
		 
	}
	
	@Test
	public void test_null_value_for_decryption() throws Exception {
		 byte[] array = new byte[7];
		 new Random().nextBytes(array);
		 String generatedString = new String(array, Charset.forName("UTF-8"));
		 String encryptedValue = encryptionService.encrypt(generatedString);
		 String decryptedValue = encryptionService.decrypt(encryptedValue);
		 
		 if(decryptedValue==null) {
			 expectedEx.expect(NullPointerException.class);
			 expectedEx.expectMessage(containsString("decrypted value cannot be null!"));
			 }
		 assertNotEquals(null,decryptedValue);
	}
	@Test
	public void test_service_decryption() throws Exception {
		 byte[] array = new byte[7];
		 new Random().nextBytes(array);
		 String generatedString = new String(array, Charset.forName("UTF-8"));
		 String encryptedValue = encryptionService.encrypt(generatedString);
		 String decryptedValue = encryptionService.decrypt(encryptedValue);
		 
		 assertEquals(generatedString,decryptedValue);
		 assertNotEquals(null,decryptedValue);
	}
	@Test
	public void test_null_value_for_encryption() throws Exception {
	
		 byte[] array = new byte[7];
		 new Random().nextBytes(array);
		 String generatedString = new String(array, Charset.forName("UTF-8"));
		 String response = encryptionService.encrypt(generatedString);
		 if(response==null) {
		 expectedEx.expect(NullPointerException.class);
		 expectedEx.expectMessage(containsString("encrypted value cannot be null!"));
		 }
		 assertNotEquals(null,response);		 
	}
	    
/*    @Test
    public void test_decryption_EncryptionException() throws Exception {
        mockServer.expect(requestTo(decryptionUri))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());
        
         mockServer.verify();
        assertThat(result, allOf(containsString("FAILED"),
                       containsString("ResourceAccessException")));
    }
    
    @Test
	public void test_encryption_EncryptionError() {
		mockServer.expect(requestTo(encryptionUri))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withServerError());
		mockServer.verify();

	}*/
  }
