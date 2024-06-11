package dev.sunbirdrc.registry.authorization;

import dev.sunbirdrc.registry.authorization.pojos.OAuth2Configuration;
import dev.sunbirdrc.registry.authorization.pojos.OAuth2Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "authentication.enabled", havingValue = "true", matchIfMissing = false)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private OAuth2Configuration oAuth2Configuration;

	@Autowired
	private SchemaAuthFilter schemaAuthFilter;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		HttpSecurity httpConfig = http.csrf().disable();
		Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();
		this.oAuth2Configuration.getResource().forEach(issuer -> addManager(authenticationManagers, issuer));
		httpConfig
				.addFilterBefore(schemaAuthFilter, WebAsyncManagerIntegrationFilter.class)
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
						.authenticationManagerResolver(new JwtIssuerAuthenticationManagerResolver(authenticationManagers::get)));

	}

	private void addManager(Map<String, AuthenticationManager> authenticationManagers, OAuth2Resource auth2Resources) {
		TenantJwtDecoder tenantJwtDecoder = CustomJwtDecoders.fromOidcIssuerLocation(auth2Resources.getUri());
		JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(tenantJwtDecoder);
		authenticationProvider.setJwtAuthenticationConverter(new CustomJwtAuthenticationConverter(auth2Resources));
		authenticationManagers.put(tenantJwtDecoder.getIssuer(), authenticationProvider::authenticate);
	}

}

