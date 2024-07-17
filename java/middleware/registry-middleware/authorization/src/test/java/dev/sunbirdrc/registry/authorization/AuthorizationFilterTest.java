package dev.sunbirdrc.registry.authorization;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.sunbirdrc.pojos.APIMessage;
import dev.sunbirdrc.registry.authorization.pojos.AuthInfo;
import dev.sunbirdrc.registry.middleware.MiddlewareHaltException;
import dev.sunbirdrc.registry.middleware.util.Constants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationFilterTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	@Mock
	private AuthorizationFilter authFilter;
	@Mock
	private APIMessage apiMessage;

	private Type type = new TypeToken<Map<String, String>>() {
	}.getType();

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

	private static void injectIntoUnmodifiableMap(String key, String value, Object map)
			throws ReflectiveOperationException {
		Class unmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
		Field field = getAccessibleField(unmodifiableMap, "m");
		Object obj = field.get(map);
		((Map<String, String>) obj).put(key, value);
	}

	@Before
	public void initialize() {
		// baseM = new AuthorizationFilter(new KeyCloakServiceImpl());
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void test_missing_auth_token() throws MiddlewareHaltException, IOException {
		expectedEx.expectMessage("Auth token is missing");
		expectedEx.expect(MiddlewareHaltException.class);
		when(authFilter.execute(apiMessage)).thenThrow(new MiddlewareHaltException("Auth token is missing"));
		authFilter.execute(apiMessage);
	}

	@Test
	public void test_valid_token() throws MiddlewareHaltException, IOException {
		String accessToken = "testToken";
		apiMessage.addLocalMap(Constants.TOKEN_OBJECT, accessToken);

		when(authFilter.execute(apiMessage)).thenReturn(true);
		authFilter.execute(apiMessage);
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
}
