package dev.sunbirdrc.registry.authorization;

import dev.sunbirdrc.registry.authorization.pojos.OAuth2Properties;
import dev.sunbirdrc.registry.authorization.pojos.UserToken;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomJwtAuthenticationConverterTest {
	@Test
	public void shouldExtractValuesFromJWTClaim() {
		OAuth2Properties oAuth2Properties = new OAuth2Properties();
		oAuth2Properties.setUserIdPath("sub");
		oAuth2Properties.setRolesPath("user.roles");
		oAuth2Properties.setConsentPath("user.consent");
		oAuth2Properties.setEmailPath("user.email");
		oAuth2Properties.setEntityPath("entity");
		CustomJwtAuthenticationConverter converter = new CustomJwtAuthenticationConverter(oAuth2Properties);
		Jwt jwt = mock(Jwt.class);
		Map<String, Object> claims = new HashMap<>();
		claims.put("sub", "1234");
		Map<String, Object> userMap = new HashMap<>();
		userMap.put("roles", Collections.singletonList("admin"));
		Map<String, Integer> consentMap = new HashMap<>();
		consentMap.put("name", 1);
		userMap.put("consent", consentMap);
		userMap.put("email", "1234@mail.com");
		claims.put("user", userMap);
		claims.put("entity", Arrays.asList("Student", "Teacher"));
		when(jwt.getClaims()).thenReturn(claims);
		AbstractAuthenticationToken authenticationToken = converter.convert(jwt);
		assert authenticationToken != null;
		Assert.assertEquals("1234", ((UserToken) authenticationToken).getUserId());
		Assert.assertEquals("{name=1}", ((UserToken) authenticationToken).getConsentFields().toString());
		Assert.assertEquals("1234@mail.com", ((UserToken) authenticationToken).getEmail());
		Assert.assertEquals("[Student, Teacher]", ((UserToken) authenticationToken).getEntities().toString());
		Assert.assertEquals("[admin]", authenticationToken.getAuthorities().toString());
	}

	@Test
	public void shouldHandleInvalidValuesWhileExtractionWithDefaultValues() {
		OAuth2Properties oAuth2Properties = new OAuth2Properties();
		oAuth2Properties.setUserIdPath("sub");
		oAuth2Properties.setRolesPath("user.roles");
		oAuth2Properties.setConsentPath("user.consent");
		oAuth2Properties.setEmailPath("user.email");
		oAuth2Properties.setEntityPath("entity");
		CustomJwtAuthenticationConverter converter = new CustomJwtAuthenticationConverter(oAuth2Properties);
		Jwt jwt = mock(Jwt.class);
		Map<String, Object> claims = new HashMap<>();
		claims.put("sub", "1234");
		Map<String, Object> userMap = new HashMap<>();
		userMap.put("roles", "admin");
		userMap.put("email", "1234@mail.com");
		claims.put("user", userMap);
		when(jwt.getClaims()).thenReturn(claims);
		AbstractAuthenticationToken authenticationToken = converter.convert(jwt);
		assert authenticationToken != null;
		Assert.assertEquals("1234", ((UserToken) authenticationToken).getUserId());
		Assert.assertEquals("{}", ((UserToken) authenticationToken).getConsentFields().toString());
		Assert.assertEquals("1234@mail.com", ((UserToken) authenticationToken).getEmail());
		Assert.assertEquals("[]", ((UserToken) authenticationToken).getEntities().toString());
		Assert.assertEquals("[]", authenticationToken.getAuthorities().toString());
	}
}
