package dev.sunbirdrc.registry.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import dev.sunbirdrc.registry.exception.SignatureException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Gson.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class SignatureServiceImplTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	@Mock
	private RetryRestTemplate retryRestTemplate;
	@Mock
    private ObjectMapper objectMapper;
	@InjectMocks
	private SignatureServiceImpl signatureServiceImpl;

	@Before
	public void setUp(){
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(signatureServiceImpl, "signatureEnabled", true);
	}

	/** Test case for sign api
	 * @throws Exception
	 */
	@Test
	public void test_sign_api() throws Exception {

        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenAnswer(new Answer<ResponseEntity<String>>(){
            @Override
            public ResponseEntity<String>  answer(InvocationOnMock invocation) throws Throwable {
                String response = "success";
                return ResponseEntity.accepted().body(response);
            }
        });
        when(objectMapper.readTree(anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
        assertThat(signatureServiceImpl.sign(Collections.emptyMap()), is(notNullValue()));
	}

    /** Test case to throw restclient exception
     * @throws Exception
     */
    @Test
    public void test_sign_api_restclient_exception() throws Exception {
        expectedEx.expect(SignatureException.UnreachableException.class);
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenThrow(RestClientException.class);
        signatureServiceImpl.sign(Collections.emptyMap());
    }

	/** Test case for verify api with simple string as value
	 * @throws Exception
	 */
	@Test
	public void test_verify_sign_with_value_as_string() throws Exception {
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenAnswer(new Answer<ResponseEntity<String>>(){
            @Override
            public ResponseEntity<String>  answer(InvocationOnMock invocation) throws Throwable {
                String response = "success";
                return ResponseEntity.accepted().body(response);
            }
        });
        ObjectNode value = JsonNodeFactory.instance.objectNode();
        value.set("verified", JsonNodeFactory.instance.booleanNode(false));
        when(objectMapper.readTree(anyString())).thenReturn(value);
        assertThat(signatureServiceImpl.verify(new Object()), is(notNullValue()));
	}

	/** Test case to throw restclient exception
	 * @throws Exception
	 */
	@Test
	public void test_verify_sign_with_restclient_exception() throws Exception {
        expectedEx.expect(SignatureException.UnreachableException.class);
        when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenThrow(RestClientException.class);
        signatureServiceImpl.verify(new Object());
	}

	/** Test case to get sign key for valid key-id
	 * @throws Exception
	 */
	@Test
	public void test_get_key_with_valid_keyId() throws Exception {
        when(retryRestTemplate.getForEntity(any(String.class))).thenAnswer(new Answer<ResponseEntity<String>>(){
            @Override
            public ResponseEntity<String>  answer(InvocationOnMock invocation) throws Throwable {
                String response = "success";
                return ResponseEntity.accepted().body(response);
            }
        });
        assertThat(signatureServiceImpl.getKey("2"), is(notNullValue()));
	}

	/** Test case to throw restclient exception
	 * @throws Exception
	 */
	@Test
	public void test_get_key_with_restclient_exception() throws Exception {
        expectedEx.expect(SignatureException.UnreachableException.class);
        when(retryRestTemplate.getForEntity(any(String.class))).thenThrow(RestClientException.class);
        signatureServiceImpl.getKey("100");
	}

    @Test
    public void test_encryption_isup() throws Exception {
        when(retryRestTemplate.getForEntity(nullable(String.class))).thenReturn(ResponseEntity.accepted().body("UP"));
        assertTrue(signatureServiceImpl.isServiceUp());
    }

    @Test
    public void test_encryption_isup_throw_restclientexception() throws Exception {
        when(retryRestTemplate.getForEntity(nullable(String.class))).thenThrow(RestClientException.class);
        assertFalse(signatureServiceImpl.isServiceUp());
    }

}
