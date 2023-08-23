package dev.sunbirdrc.registry.config;

import dev.sunbirdrc.registry.util.DefinitionsManager;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;

@Configuration
@EnableWebSecurity
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
@ConditionalOnProperty(name = "authentication.enabled",havingValue = "true",matchIfMissing = false)
public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

    @Value("${authentication.enabled:true}") boolean authenticationEnabled;

    @Autowired
    private SchemaFilter schemaFilter;
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

        KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(
                new SimpleAuthorityMapper());
        auth.authenticationProvider(keycloakAuthenticationProvider);
    }

    @Bean
    public KeycloakSpringBootConfigResolver KeycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

    @Bean
    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(
                new SessionRegistryImpl());
    }

    //TODO: verify all paths
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        HttpSecurity httpConfig = http.csrf().disable();
        if (authenticationEnabled) {
            httpConfig.authorizeRequests()
                    .antMatchers("/**/invite","/**/v2/{entityName}/claims", "/health", "/error",
                            "/_schemas/**", "/**/templates/**", "/**/*.json", "/**/verify",
                            "/swagger-ui", "/**/search", "/**/attestation/**",
                            "/api/docs/swagger.json","/api/docs/*.json", "/plugin/**", "/swagger-ui.html","/api/v1/pullUriRequest/*","/api/v1/pullDocUriRequest/*")
                    .permitAll()
                    .and()
                    //.addFilterBefore(schemaFilter, WebAsyncManagerIntegrationFilter.class)
                    .authorizeRequests()
                    .anyRequest()
                    .authenticated();
        } else {
            httpConfig.authorizeRequests()
                    .anyRequest()
                    .permitAll();
        }
    }
}
