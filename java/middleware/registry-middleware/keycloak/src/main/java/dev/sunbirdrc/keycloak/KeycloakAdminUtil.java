package dev.sunbirdrc.keycloak;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.*;
import javax.ws.rs.core.Response;


@Component
@PropertySource(value = "classpath:application.yml", ignoreResourceNotFound = true)
public class KeycloakAdminUtil {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminUtil.class);
    private static final String EMAIL = "email_id";
    private static final String ENTITY = "entity";
    private static final String MOBILE_NUMBER = "mobile_number";
    private static final String PASSWORD = "password";


    private String realm;
    private String adminClientSecret;
    private String adminClientId;
    private String authURL;
    private String defaultPassword;
    private boolean setDefaultPassword;
    private List<String> emailActions;
    private final Keycloak keycloak;

    @Autowired
    public KeycloakAdminUtil(
            @Value("${keycloak.realm:}") String realm,
            @Value("${keycloak-admin.client-secret:}") String adminClientSecret,
            @Value("${keycloak-admin.client-id:}") String adminClientId,
            @Value("${keycloak-user.default-password:}") String defaultPassword,
            @Value("${keycloak-user.set-default-password:false}") boolean setDefaultPassword,
            @Value("${keycloak.auth-server-url:}") String authURL,
            @Value("${keycloak-user.email-actions:}") List<String> emailActions) {
        this.realm = realm;
        this.adminClientSecret = adminClientSecret;
        this.adminClientId = adminClientId;
        this.authURL = authURL;
        this.defaultPassword = defaultPassword;
        this.setDefaultPassword = setDefaultPassword;
        this.emailActions = emailActions;
        this.keycloak = buildKeycloak();
    }

    private Keycloak buildKeycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(authURL)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(adminClientId)
                .clientSecret(adminClientSecret)
                .build();
    }

    public String createUser(String entityName, String userName, String email, String mobile) throws OwnerCreationException {
        logger.info("Creating user with mobile_number : " + userName);
        UserRepresentation newUser = createUserRepresentation(entityName, userName, email, mobile);
        GroupRepresentation entityGroup = createGroupRepresentation(entityName);
        keycloak.realm(realm).groups().add(entityGroup);
        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(newUser);
        if (response.getStatus() == 201) {
            logger.info("Response |  Status: {} | Status Info: {}", response.getStatus(), response.getStatusInfo());
            logger.info("User ID path" + response.getLocation().getPath());
            String userID = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            logger.info("User ID : " + userID);
            if(!emailActions.isEmpty())
                usersResource.get(userID).executeActionsEmail(emailActions);
            return userID;
        } else if (response.getStatus() == 409) {
            logger.info("UserID: {} exists", userName);
            return updateExistingUserAttributes(entityName, userName, email, mobile);
        } else if (response.getStatus() == 500) {
            throw new OwnerCreationException("Keycloak user creation error");
        }else {
            throw new OwnerCreationException("Username already invited / registered");
        }
    }

    private GroupRepresentation createGroupRepresentation(String entityName) {
        GroupRepresentation groupRepresentation = new GroupRepresentation();
        groupRepresentation.setName(entityName);
        return groupRepresentation;
    }

    private String updateExistingUserAttributes(String entityName, String userName, String email, String mobile) throws OwnerCreationException {
        Optional<UserResource> userRepresentationOptional = getUserByUsername(userName);
        if (userRepresentationOptional.isPresent()) {
            UserResource userResource = userRepresentationOptional.get();
            UserRepresentation userRepresentation = userResource.toRepresentation();
            checkIfUserRegisteredForEntity(entityName, userRepresentation);
            updateUserAttributes(entityName, email, mobile, userRepresentation);
            userResource.update(userRepresentation);
            return userRepresentation.getId();
        } else {
            logger.error("Failed fetching user by username: {}", userName);
            throw new OwnerCreationException("Creating user failed");
        }
    }

    private UserRepresentation createUserRepresentation(String entityName, String userName, String email, String mobile) {
        UserRepresentation newUser = new UserRepresentation();
        newUser.setEnabled(true);
        newUser.setUsername(userName);
        if (setDefaultPassword) {
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setValue(this.defaultPassword);
            credentialRepresentation.setType(PASSWORD);
            newUser.setCredentials(Collections.singletonList(credentialRepresentation));
        }
        newUser.setGroups(Collections.singletonList(entityName));
        newUser.setEmail(email);
        newUser.singleAttribute(MOBILE_NUMBER, mobile);
        newUser.singleAttribute(EMAIL, email);
        newUser.singleAttribute(ENTITY, entityName);
        return newUser;
    }

    private void updateUserAttributes(String entityName, String email, String mobile, UserRepresentation userRepresentation) {
        List<String> entities = userRepresentation.getAttributes().getOrDefault(ENTITY, Collections.emptyList());
        entities.add(entityName);
        addAttributeIfNotExists(userRepresentation, EMAIL, email);
        addAttributeIfNotExists(userRepresentation, MOBILE_NUMBER, mobile);
    }

    private void addAttributeIfNotExists(UserRepresentation userRepresentation, String key, String value) {
        if (!userRepresentation.getAttributes().containsKey(key)) {
            userRepresentation.singleAttribute(key, value);
        }
    }

    private void checkIfUserRegisteredForEntity(String entityName, UserRepresentation userRepresentation) throws OwnerCreationException {
        List<String> entities = userRepresentation.getAttributes().getOrDefault(ENTITY, Collections.emptyList());
        if (!entities.isEmpty() && entities.contains(entityName)) {
            throw new OwnerCreationException("Username already invited / registered for " + entityName);
        }
    }

    private Optional<UserResource> getUserByUsername(String username) {
        List<UserRepresentation> users = keycloak.realm(realm).users().search(username);
        if (users.size() > 0) {
            return Optional.of(keycloak.realm(realm).users().get(users.get(0).getId()));
        }
        return Optional.empty();
    }

    private void addUserToGroup(String groupName, UserRepresentation user) {
        keycloak.realm(realm).groups().groups().stream()
                .filter(g -> g.getName().equals(groupName)).findFirst()
                .ifPresent(g -> keycloak.realm(realm).users().get(user.getId()).joinGroup(g.getId()));
    }
}
