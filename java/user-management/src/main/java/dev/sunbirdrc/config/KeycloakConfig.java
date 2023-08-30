package dev.sunbirdrc.config;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;


@Configuration
public class KeycloakConfig {
    @Autowired
    private PropertiesValueMapper valueMapper;

    @Primary
    @Bean(name = "systemKeycloak")
    public Keycloak systemKeycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(valueMapper.getKeycloakServerUrl())
                .realm(valueMapper.getRealm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(valueMapper.getConfidentialClientId())
                .clientSecret(valueMapper.getClientSecret())
                .resteasyClient(new ResteasyClientBuilder()
                        .connectionPoolSize(10)
                        .build()
                )
                .build();
    }

    public Keycloak getUserKeycloak(String username, String password) {
        return KeycloakBuilder.builder()
                .serverUrl(valueMapper.getKeycloakServerUrl())
                .realm(valueMapper.getRealm())
                .grantType(OAuth2Constants.PASSWORD)
                .username(username)
                .password(password)
                .clientId(valueMapper.getPublicClientId())
                .resteasyClient(new ResteasyClientBuilder()
                        .connectionPoolSize(10)
                        .build()
                )
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
