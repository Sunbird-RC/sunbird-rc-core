package io.opensaber.registry.util;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import javax.ws.rs.core.Response;


@Component
public class KeycloakAdminUtil {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminUtil.class);

    private static final String realm = "divoc";
    private static final String clientSecret = "daa3e6b4-7be0-4cc5-9854-14ea15f74cae";
    private static final String authURL = "http://localhost:8081/auth";

    @Autowired
    public KeycloakAdminUtil() { }

    private final Keycloak keycloak = KeycloakBuilder.builder()
            .serverUrl(authURL)
            .realm("divoc")
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId("admin-api")
            .clientSecret(clientSecret)
            .build();

    public void createUser(String mobileNumber, String role) {
        logger.info("Creating user with mobile_number : " + mobileNumber);
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(mobileNumber);
        user.setAttributes(
            Collections.singletonMap("mobile_number",
            Collections.singletonList(mobileNumber))
        );
        user.setGroups(Collections.singletonList(role));
        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(user);
        logger.info("Create User response status "+ response.getStatusInfo().toString());

    }
}
