package io.opensaber.registry.service.impl;

import com.google.gson.Gson;
import io.opensaber.pojos.OpenSaberInstrumentation;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.middleware.util.Constants;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class EncryptionServiceImplTest{

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	@Mock
	private RetryRestTemplate retryRestTemplate;
	@Mock
	private Gson gson;
	@Mock
	public OpenSaberInstrumentation watch;
	@InjectMocks
	private EncryptionServiceImpl encryptionServiceImpl;

	@Before
	public void setUp(){
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void test_encrypt_api_with_object_as_input() throws Exception {
		when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenAnswer(new Answer<ResponseEntity<String>>(){
			@Override
			public ResponseEntity<String>  answer(InvocationOnMock invocation) throws Throwable {
				String response = "success";
				return ResponseEntity.accepted().body(response);
			}
		});
		assertThat(encryptionServiceImpl.encrypt(new Object()), is(notNullValue()));
	}

	@Test
	public void test_encrypted_api_with_map_as_input() throws Exception {
		when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenAnswer(new Answer<ResponseEntity<String>>(){
			@Override
			public ResponseEntity<String>  answer(InvocationOnMock invocation) throws Throwable {
				Map responseMap = new HashMap();
				responseMap.put("A","1");
				responseMap.put("B","2");
				return ResponseEntity.accepted().body(responseMap.toString());
			}
		});

		when(gson.toJson(any(Object.class))).thenReturn(new String());
		when(gson.fromJson(any(String.class),any(Type.class))).thenReturn(new HashMap<String, Object>());
		doNothing().when(watch).start(isA(String.class));
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		propertyMap.put("school", "BVM");
		propertyMap.put("name", "john");
		assertThat(encryptionServiceImpl.encrypt(propertyMap), is(notNullValue()));
	}

	@Test(expected = EncryptionException.class)
	public void test_encrypt_api_object_param_throwing_resource_exception() throws Exception {
		when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenThrow(ResourceAccessException.class);
		assertThat(encryptionServiceImpl.encrypt(new Object()), is(notNullValue()));
	}

	@Test(expected = EncryptionException.class)
	public void test_encrypt_api_map_param_throwing_resource_exception() throws Exception {
		when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenThrow(ResourceAccessException.class);
		encryptionServiceImpl.encrypt(new HashMap<>());
	}

	@Test
	public void test_decrypt_api_with_object_as_input() throws Exception {
		when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenAnswer(new Answer<ResponseEntity<String>>(){
			@Override
			public ResponseEntity<String>  answer(InvocationOnMock invocation) throws Throwable {
				String response = "success";
				return ResponseEntity.accepted().body(response);
			}
		});
		assertThat(encryptionServiceImpl.decrypt(new Object()), is(notNullValue()));
	}

	@Test(expected = EncryptionException.class)
	public void test_decrypt_api_object_param_throwing_resource_exception() throws Exception {
		when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenThrow(ResourceAccessException.class);
		encryptionServiceImpl.decrypt(new Object());
	}

	@Test
	public void test_decrypt_api_with_input_as_map() throws Exception {
		when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenAnswer(new Answer<ResponseEntity<String>>(){
			@Override
			public ResponseEntity<String>  answer(InvocationOnMock invocation) throws Throwable {
				Map responseMap = new HashMap();
				responseMap.put("A","1");
				responseMap.put("B","2");
				return ResponseEntity.accepted().body(responseMap.toString());
			}
		});

		when(gson.toJson(any(Object.class))).thenReturn(new String());
		when(gson.fromJson(any(String.class),any(Type.class))).thenReturn(new HashMap<String, Object>());
		doNothing().when(watch).start(isA(String.class));
		Map<String, Object> propertyMap = new HashMap<String, Object>();
		propertyMap.put("school", "BVM");
		propertyMap.put("name", "john");
		assertThat(encryptionServiceImpl.decrypt(propertyMap), is(notNullValue()));
	}

	@Test(expected = EncryptionException.class)
	public void test_decrypt_api_map_param_throwing_resource_exception() throws Exception {
		when(retryRestTemplate.postForEntity(nullable(String.class), any(Object.class))).thenThrow(ResourceAccessException.class);
		encryptionServiceImpl.decrypt(new HashMap<>());
	}

	@Test
	public void test_encryption_isup() throws Exception {
		when(retryRestTemplate.getForEntity(nullable(String.class))).thenReturn(ResponseEntity.accepted().body("UP"));
		assertTrue(encryptionServiceImpl.isEncryptionServiceUp());
	}

	@Test
	public void test_encryption_isup_throw_restclientexception() throws Exception {
		when(retryRestTemplate.getForEntity(nullable(String.class))).thenThrow(RestClientException.class);
		assertFalse(encryptionServiceImpl.isEncryptionServiceUp());
	}

}
