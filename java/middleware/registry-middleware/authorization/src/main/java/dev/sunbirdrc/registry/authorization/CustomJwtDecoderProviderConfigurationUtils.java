package dev.sunbirdrc.registry.authorization;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

final class CustomJwtDecoderProviderConfigurationUtils {
    private static final String OIDC_METADATA_PATH = "/.well-known/openid-configuration";
    private static final RestTemplate rest = new RestTemplate();
    private static final ParameterizedTypeReference<Map<String, Object>> typeReference =
            new ParameterizedTypeReference<Map<String, Object>>() {};

    static Map<String, Object> getConfigurationForOidcIssuerLocation(String oidcIssuerLocation) {
        return getConfiguration(oidcIssuerLocation, oidc(URI.create(oidcIssuerLocation)));
    }

    static String getIssuer(Map<String, Object> configuration) {
        String metadataIssuer = "(unavailable)";
        if (configuration.containsKey("issuer")) {
            metadataIssuer = configuration.get("issuer").toString();
        }
        return metadataIssuer;
    }

    private static Map<String, Object> getConfiguration(String issuer, URI... uris) {
        String errorMessage = "Unable to resolve the Configuration with the provided Issuer of " +
                "\"" + issuer + "\"";
        for (URI uri : uris) {
            try {
                RequestEntity<Void> request = RequestEntity.get(uri).build();
                ResponseEntity<Map<String, Object>> response = rest.exchange(request, typeReference);
                Map<String, Object> configuration = response.getBody();

                if (configuration.get("jwks_uri") == null) {
                    throw new IllegalArgumentException("The public JWK set URI must not be null");
                }

                return configuration;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (RuntimeException e) {
                if (!(e instanceof HttpClientErrorException &&
                        ((HttpClientErrorException) e).getStatusCode().is4xxClientError())) {
                    throw new IllegalArgumentException(errorMessage, e);
                }
                // else try another endpoint
            }
        }
        throw new IllegalArgumentException(errorMessage);
    }

    private static URI oidc(URI issuer) {
        return UriComponentsBuilder.fromUri(issuer)
                .replacePath(issuer.getPath() + OIDC_METADATA_PATH)
                .build(Collections.emptyMap());
    }
}

