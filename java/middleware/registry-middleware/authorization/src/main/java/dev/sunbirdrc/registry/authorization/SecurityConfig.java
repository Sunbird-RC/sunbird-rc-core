package dev.sunbirdrc.registry.authorization;

import dev.sunbirdrc.registry.authorization.pojos.OAuth2Configuration;
import dev.sunbirdrc.registry.authorization.pojos.OAuth2Resources;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "authentication.enabled", havingValue = "true", matchIfMissing = false)
public class SecurityConfig {

    @Autowired
    private OAuth2Configuration oAuth2Configuration;

    @Autowired
    private SchemaAuthFilter schemaAuthFilter;

    public static class InviteRequestMatcher implements RequestMatcher {
        private final Pattern[] patterns;

        public InviteRequestMatcher(String... regexes) {
            patterns = new Pattern[regexes.length];
            for (int i = 0; i < regexes.length; i++) {
                patterns[i] = Pattern.compile(regexes[i]);
            }
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            String uri = request.getRequestURI();
            for (Pattern pattern : patterns) {
                if (pattern.matcher(uri).find()) {
                    return true;
                }
            }
            return false;
        }
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();
        oAuth2Configuration.getResources().forEach(issuer -> addManager(authenticationManagers, issuer));
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(new InviteRequestMatcher(
                                        ".*/invite$", ".*/health", ".*/error", ".*/_schemas/.+$", ".*/templates/.+$",
                                        ".*/search", "^.+\\.json$", ".*//swagger-ui$", ".*/attestation/.+$",
                                        ".*/plugin/.+$", ".*/swagger-ui.html$"))
                                .permitAll()
                                .anyRequest()
                                .permitAll()
                )
                .addFilterBefore(schemaAuthFilter, WebAsyncManagerIntegrationFilter.class)
                .httpBasic(Customizer.withDefaults()
                ).oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
                        .authenticationManagerResolver(new JwtIssuerAuthenticationManagerResolver(authenticationManagers::get)));
        return http.build();
    }

    private void addManager(Map<String, AuthenticationManager> authenticationManagers, OAuth2Resources auth2Resources) {
        TenantJwtDecoder tenantJwtDecoder = CustomJwtDecoders.fromOidcIssuerLocation(auth2Resources.getUri());
        JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(tenantJwtDecoder);
        authenticationProvider.setJwtAuthenticationConverter(new CustomJwtAuthenticationConverter(auth2Resources.getProperties()));
        authenticationManagers.put(tenantJwtDecoder.getIssuer(), authenticationProvider::authenticate);
    }
}