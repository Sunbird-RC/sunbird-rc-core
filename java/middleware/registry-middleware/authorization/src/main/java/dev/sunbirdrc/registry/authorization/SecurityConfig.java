package dev.sunbirdrc.registry.authorization;

import dev.sunbirdrc.registry.authorization.pojos.OAuth2Configuration;
import dev.sunbirdrc.registry.authorization.pojos.OAuth2Resources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "authentication.enabled",havingValue = "true",matchIfMissing = false)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${authentication.enabled:true}")
	boolean authenticationEnabled;

	@Autowired
	private OAuth2Configuration oAuth2Configuration;

	@Autowired
	private SchemaAuthFilter schemaAuthFilter;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		HttpSecurity httpConfig = http.csrf().disable();
		if (authenticationEnabled) {
			Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();
			this.oAuth2Configuration.getResources().forEach(issuer -> addManager(authenticationManagers, issuer));
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
		} else {
			httpConfig.authorizeRequests(auth -> auth
					.anyRequest()
					.permitAll()
			);
		}

	}

	private void addManager(Map<String, AuthenticationManager> authenticationManagers, OAuth2Resources issuer) {
		JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(JwtDecoders.fromOidcIssuerLocation(issuer.getUri()));
		authenticationProvider.setJwtAuthenticationConverter(new CustomJwtAuthenticationConverter(issuer.getProperties()));
		authenticationManagers.put(issuer.getUri(), authenticationProvider::authenticate);
	}

}

