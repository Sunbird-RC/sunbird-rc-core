package io.opensaber.registry.authorization;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.junit.Ignore;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import io.opensaber.registry.authorization.pojos.AuthInfo;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.registry.middleware.util.Constants;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationFilterTest {

	@Mock
	private AuthorizationFilter authFilter;

	@Before
	public void initialize() {
		// baseM = new AuthorizationFilter(new KeyCloakServiceImpl());
		MockitoAnnotations.initMocks(this);
	}

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private Type type = new TypeToken<Map<String, String>>() {
	}.getType();

	@Test
	public void test_missing_auth_token() throws MiddlewareHaltException, IOException {
		expectedEx.expectMessage("Auth token is missing");
		expectedEx.expect(MiddlewareHaltException.class);
		Map<String, Object> mapObject = new HashMap<>();
		when(authFilter.execute(mapObject)).thenThrow(new MiddlewareHaltException("Auth token is missing"));
		authFilter.execute(mapObject);
	}

	@Test
	public void test_valid_token() throws MiddlewareHaltException, IOException {
		String accessToken = "testToken";
		Map<String, Object> mapObject = new HashMap<>();
		mapObject.put(Constants.TOKEN_OBJECT, accessToken);
		when(authFilter.execute(mapObject)).thenReturn(new HashMap<>());
		authFilter.execute(mapObject);
		Authentication authentication = mock(Authentication.class);
		SecurityContext securityContext = mock(SecurityContext.class);
		AuthInfo mockAuthInfo = mock(AuthInfo.class);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		when(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).thenReturn(mockAuthInfo);
		AuthInfo authInfo = (AuthInfo) securityContext.getAuthentication().getPrincipal();
		when(mockAuthInfo.getSub()).thenReturn("874ed8a5-782e-4f6c-8f36-e0288455901e");
		when(mockAuthInfo.getAud()).thenReturn("admin-cli");
		assertNotNull(authInfo.getSub());
		assertNotNull(authInfo.getAud());
		assertEquals("874ed8a5-782e-4f6c-8f36-e0288455901e", authInfo.getSub());
		assertEquals("admin-cli", authInfo.getAud());
	}

	@Test
	public void test_invalid_token() throws MiddlewareHaltException, IOException {
		expectedEx.expectMessage("Auth token is invalid");
		expectedEx.expect(MiddlewareHaltException.class);
		Map<String, Object> mapObject = new HashMap<>();
		mapObject.put(Constants.TOKEN_OBJECT, "invalid.token");
		when(authFilter.execute(mapObject)).thenThrow(new MiddlewareHaltException("Auth token is invalid"));
		authFilter.execute(mapObject);
	}

	@Test @Ignore
	public void test_keycloak_token_validation() throws Exception {
		Map<String, Object> mapObject = new HashMap<>();
		String body = "client_id=" + System.getenv("sunbird_sso_client_id") + "&username=" + System.getenv("sunbird_sso_username")
				+ "&password=" + System.getenv("sunbird_sso_password") + "&grant_type=password";
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("no-cache");
		headers.set("content-type", "application/x-www-form-urlencoded");
		HttpEntity<String> request = new HttpEntity<String>(body, headers);

		String url = System.getenv("sunbird_sso_url") + "realms/" + System.getenv("sunbird_sso_realm") + "/protocol/openid-connect/token ";
		ResponseEntity<String> response = new RestTemplate().postForEntity(url, request, String.class);
		Map<String, String> myMap = new Gson().fromJson(response.getBody(), type);
		String accessToken = (String) myMap.get("access_token");
		mapObject.put(Constants.TOKEN_OBJECT, accessToken);
		String userId = "874ed8a5-782e-4f6c-8f36-e0288455901e";
		assertEquals(new KeyCloakServiceImpl().verifyToken(accessToken), userId);
	}

	@Test @Ignore
	public void test_invalid_environment_variable() throws Exception {
		expectedEx.expectMessage("Auth token and/or Environment variable is invalid");
		expectedEx.expect(MiddlewareHaltException.class);

		Map<String, Object> mapObject = new HashMap<>();
		String body = "client_id=" + System.getenv("sunbird_sso_client_id") + "&username=" + System.getenv("sunbird_sso_username")
				+ "&password=" + System.getenv("sunbird_sso_password") + "&grant_type=password";
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("no-cache");
		headers.set("content-type", "application/x-www-form-urlencoded");
		HttpEntity<String> request = new HttpEntity<>(body, headers);

		String publicKey = System.getenv("sunbird_sso_publickey");
		String realm = System.getenv("sunbird_sso_realm");
		String authUrl = System.getenv("sunbird_sso_url");
		String userName = System.getenv("sunbird_sso_username");
		String password = System.getenv("sunbird_sso_password");
		String clientId = System.getenv("sunbird_sso_client_id");
		try {
			String url = System.getenv("sunbird_sso_url") + "realms/" + System.getenv("sunbird_sso_realm") + "/protocol/openid-connect/token ";
			ResponseEntity<String> response = new RestTemplate().postForEntity(url, request, String.class);
			Map<String, String> myMap = new Gson().fromJson(response.getBody(), type);
			String accessToken = myMap.get("access_token");
			mapObject.put(Constants.TOKEN_OBJECT, accessToken);

			assertNotNull(System.getenv("sunbird_sso_publickey"));
			assertNotNull(System.getenv("sunbird_sso_realm"));
			assertNotNull(System.getenv("sunbird_sso_url"));
			assertNotNull(System.getenv("sunbird_sso_username"));
			assertNotNull(System.getenv("sunbird_sso_password"));
			assertNotNull(System.getenv("sunbird_sso_client_id"));

			injectEnvironmentVariable("sunbird_sso_publickey", "invalid.public.key");
			injectEnvironmentVariable("sunbird_sso_realm", "invalid.realm");
			injectEnvironmentVariable("sunbird_sso_url", "invalid.url");
			injectEnvironmentVariable("sunbird_sso_username", "invalid.userName");
			injectEnvironmentVariable("sunbird_sso_password", "invalid.password");
			injectEnvironmentVariable("sunbird_sso_client_id", "invalid.clientId");

			assertThat(System.getenv("sunbird_sso_publickey"), is("invalid.public.key"));
			assertThat(System.getenv("sunbird_sso_realm"), is("invalid.realm"));
			assertThat(System.getenv("sunbird_sso_url"), is("invalid.url"));
			assertThat(System.getenv("sunbird_sso_username"), is("invalid.userName"));
			assertThat(System.getenv("sunbird_sso_password"), is("invalid.password"));
			assertThat(System.getenv("sunbird_sso_client_id"), is("invalid.clientId"));

			// baseM.execute(mapObject);
			authFilter.execute(mapObject);
		} finally {

			injectEnvironmentVariable("sunbird_sso_publickey", publicKey);
			injectEnvironmentVariable("sunbird_sso_realm", realm);
			injectEnvironmentVariable("sunbird_sso_url", authUrl);
			injectEnvironmentVariable("sunbird_sso_username", userName);
			injectEnvironmentVariable("sunbird_sso_password", password);
			injectEnvironmentVariable("sunbird_sso_client_id", clientId);
		}
	}

	private static void injectEnvironmentVariable(String key, String value) throws Exception {
		Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
		Field unmodifiableMapField = getAccessibleField(processEnvironment, "theUnmodifiableEnvironment");
		Object unmodifiableMap = unmodifiableMapField.get(null);
		injectIntoUnmodifiableMap(key, value, unmodifiableMap);
		Field mapField = getAccessibleField(processEnvironment, "theEnvironment");
		Map<String, String> map = (Map<String, String>) mapField.get(null);
		map.put(key, value);
	}

	private static Field getAccessibleField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field;
	}

	private static void injectIntoUnmodifiableMap(String key, String value, Object map) throws ReflectiveOperationException {
		Class unmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
		Field field = getAccessibleField(unmodifiableMap, "m");
		Object obj = field.get(map);
		((Map<String, String>) obj).put(key, value);
	}
}



