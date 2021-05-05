package io.opensaber.registry.util;

import org.apache.catalina.User;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.swing.text.html.Option;
import javax.ws.rs.core.Response;


@Component
public class KeycloakAdminUtil {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminUtil.class);


    private String realm;
    private String clientSecret;
    private String authURL;
    private final Keycloak keycloak;

    @Autowired
    public KeycloakAdminUtil(
            @Value("${keycloak.sso.realm}") String realm,
            @Value("${keycloak.sso.client_secret}") String clientSecret,
            @Value("${keycloak.sso.auth_server_url}") String authURL) {
        this.realm = realm;
        this.clientSecret = clientSecret;
        this.authURL = authURL;
        this.keycloak = buildKeycloak();
    }

    private Keycloak buildKeycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(authURL)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId("admin-api")
                .clientSecret(clientSecret)
                .build();
    }

    public String createUser(String mobileNumber, String role) {
        logger.info("Checking if user already exists");
        Optional<UserRepresentation> user = getUserByUsername(mobileNumber);
        if (user.isPresent()) {
            UserRepresentation existingUser = user.get();
            logger.info("UserID: {} exists. Joining: {}", existingUser.getId(), role);
            addUserToGroup(role, existingUser);
            return existingUser.getId();
        }

        logger.info("Creating user with mobile_number : " + mobileNumber);
        UserRepresentation newUser = new UserRepresentation();
        newUser.setEnabled(true);
        newUser.setUsername(mobileNumber);
        newUser.setAttributes(
            Collections.singletonMap("mobile_number",
            Collections.singletonList(mobileNumber))
        );
        newUser.setGroups(Collections.singletonList(role));
        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(newUser);
        logger.info("Response |  Status: {} | Status Info: {}", response.getStatus(), response.getStatusInfo());
        logger.info("User ID path" + response.getLocation().getPath());
        String userID = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        logger.info("User ID : " + userID);
        return userID;
    }

    private Optional<UserRepresentation> getUserByUsername(String username) {
        List<UserRepresentation> users = keycloak.realm(realm).users().search(username);
        if (users.size() > 0) {
            return Optional.of(users.get(0));
        }
        return Optional.empty();
    }

    private void addUserToGroup(String groupName, UserRepresentation user) {
        keycloak.realm(realm).groups().groups().stream()
                .filter(g -> g.getName().equals(groupName)).findFirst()
                .ifPresent(g -> keycloak.realm(realm).users().get(user.getId()).joinGroup(g.getId()));
    }
}
