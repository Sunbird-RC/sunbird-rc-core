package dev.sunbirdrc.registry.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${authentication.enabled:true}")
	boolean authenticationEnabled;

	@Autowired
	private SchemaFilter schemaFilter;

	private JsonNode issuerNode;

	private final Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();

	private final JwtIssuerAuthenticationManagerResolver authenticationManagerResolver = new JwtIssuerAuthenticationManagerResolver(authenticationManagers::get);

	@Autowired
	private ObjectMapper objectMapper = new ObjectMapper();


	public SecurityConfig() throws JsonProcessingException {
		String env = "{\n" +
				"  \"http://keycloak:8080/auth/realms/sunbird-rc\": {\n" +
				"    \"roles\": \"realm_access.roles\",\n" +
				"    \"email\": \"email\"\n" +
				"  },\n" +
				"  \"http://keycloak:8080/auth/realms/demo\": {\n" +
				"    \"roles\": \"realm_access.roles\"\n" +
				"  },\n" +
				"  \"https://demo-education-registry.xiv.in/auth/realms/sunbird-rc\": {\n" +
				"    \"roles\": \"realm_access.roles\",\n" +
				"    \"email\": \"email\",\n" +
				"    \"consent\": \"consent\"\n" +
				"  }\n" +
				"}";
		this.issuerNode = objectMapper.readTree(env);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		HttpSecurity httpConfig = http.csrf().disable();
		if (authenticationEnabled) {
			issuerNode.fieldNames().forEachRemaining(issuer -> addManager(authenticationManagers, issuer));
			httpConfig
					.addFilterBefore(schemaFilter, WebAsyncManagerIntegrationFilter.class)
					.authorizeRequests(auth -> auth
							.antMatchers("/**/invite", "/health", "/error",
									"/_schemas/**", "/**/templates/**", "/**/*.json", "/**/verify",
									"/swagger-ui", "/**/search", "/**/attestation/**",
									"/api/docs/swagger.json", "/api/docs/*.json", "/plugin/**", "/swagger-ui.html")
							.permitAll()
					)
					.authorizeRequests(auth -> auth
							.anyRequest()
							.authenticated())
					.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
							.authenticationManagerResolver(this.authenticationManagerResolver));
		} else {
			httpConfig.authorizeRequests(auth -> auth
					.anyRequest()
					.permitAll()
			);
		}

	}

	public void addManager(Map<String, AuthenticationManager> authenticationManagers, String issuer) {
		JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(JwtDecoders.fromOidcIssuerLocation(issuer));
		authenticationProvider.setJwtAuthenticationConverter(new CustomJwtAuthenticationConverter(issuerNode.get(issuer)));
		authenticationManagers.put(issuer, authenticationProvider::authenticate);
	}

}

