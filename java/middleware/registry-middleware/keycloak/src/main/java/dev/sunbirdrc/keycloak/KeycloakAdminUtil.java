package dev.sunbirdrc.keycloak;

import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthIndicator;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.*;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_KEYCLOAK_SERVICE_NAME;


@Component
@PropertySource(value = "classpath:application.yml", ignoreResourceNotFound = true)
public class KeycloakAdminUtil implements HealthIndicator {
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
    private boolean authenticationEnabled;

    @Autowired
    public KeycloakAdminUtil(
            @Value("${authentication.enabled}")
            boolean authenticationEnabled,
            @Value("${keycloak.realm:}") String realm,
            @Value("${keycloak-admin.client-secret:}") String adminClientSecret,
            @Value("${keycloak-admin.client-id:}") String adminClientId,
            @Value("${keycloak-user.default-password:}") String defaultPassword,
            @Value("${keycloak-user.set-default-password:false}") boolean setDefaultPassword,
            @Value("${keycloak.auth-server-url:}") String authURL,
            @Value("${keycloak-user.email-actions:}") List<String> emailActions,
            @Value("${httpConnection.maxConnections:5}") int httpMaxConnections) {
        this.authenticationEnabled = authenticationEnabled;
        this.realm = realm;
        this.adminClientSecret = adminClientSecret;
        this.adminClientId = adminClientId;
        this.authURL = authURL;
        this.defaultPassword = defaultPassword;
        this.setDefaultPassword = setDefaultPassword;
        this.emailActions = emailActions;
        this.keycloak = buildKeycloak(httpMaxConnections);
    }

    private Keycloak buildKeycloak(int httpMaxConnections) {
        return KeycloakBuilder.builder()
                .serverUrl(authURL)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(adminClientId)
                .clientSecret(adminClientSecret)
                .resteasyClient(
                        new ResteasyClientBuilder()
                                .connectionPoolSize(httpMaxConnections).build()
                )
                .build();
    }

    public String createUser(String entityName, String userName, String email, String mobile) throws OwnerCreationException {
        logger.info("Creating user with mobile_number : " + userName);
        String groupId = createOrUpdateRealmGroup(entityName);
        UserRepresentation newUser = createUserRepresentation(entityName, userName, email, mobile);
        UsersResource usersResource = keycloak.realm(realm).users();
        try (Response response = usersResource.create(newUser)) {
            if (response.getStatus() == 201) {
                logger.info("Response |  Status: {} | Status Info: {}", response.getStatus(), response.getStatusInfo());
                logger.info("User ID path" + response.getLocation().getPath());
                String userID = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                logger.info("User ID : " + userID);
                if (!emailActions.isEmpty())
                    usersResource.get(userID).executeActionsEmail(emailActions);
                return userID;
            } else if (response.getStatus() == 409) {
                logger.info("UserID: {} exists", userName);
                return updateExistingUserAttributes(entityName, userName, email, mobile, groupId);
            } else if (response.getStatus() == 500) {
                throw new OwnerCreationException("Keycloak user creation error");
            } else {
                throw new OwnerCreationException("Username already invited / registered");
            }
        }
    }

    private String createOrUpdateRealmGroup(String entityName) {
        RoleRepresentation roleRepresentation = createOrGetRealmRole(entityName);
        GroupsResource groupsResource = keycloak.realm(realm).groups();
        GroupRepresentation groupRepresentation = new GroupRepresentation();
        groupRepresentation.setName(entityName);
        Response groupAddResponse = groupsResource.add(groupRepresentation);
        String groupId = "";
        if (groupAddResponse.getStatus() == 409) {
            Optional<GroupRepresentation> groupRepresentationOptional = groupsResource.groups().stream().filter(gp -> gp.getName().equalsIgnoreCase(entityName)).findFirst();
            if (groupRepresentationOptional.isPresent()) {
                groupId = groupRepresentationOptional.get().getId();
            }
        } else {
            groupId = groupAddResponse.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        }
        groupsResource.group(groupId)
                .roles().realmLevel().add(Collections.singletonList(roleRepresentation));
        return groupId;
    }

    private RoleRepresentation createOrGetRealmRole(String entityName) {
        RolesResource rolesResource = keycloak.realm(realm).roles();
        try {
            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setName(entityName);
            rolesResource.create(roleRepresentation);
        } catch (Exception e){
            logger.error("Role creation exception", e);
        }
        return rolesResource.get(entityName).toRepresentation();
    }

    private String updateExistingUserAttributes(String entityName, String userName, String email, String mobile,
                                                String groupId) throws OwnerCreationException {
        Optional<UserResource> userRepresentationOptional = getUserByUsername(userName);
        if (userRepresentationOptional.isPresent()) {
            UserResource userResource = userRepresentationOptional.get();
            UserRepresentation userRepresentation = userResource.toRepresentation();
            updateUserAttributes(entityName, email, mobile, userRepresentation);
            List<String> groups = userRepresentation.getGroups();
            if (groups == null) {
                groups = new ArrayList<>();
            }
            if (!groups.contains(entityName)) {
                groups.add(entityName);
                userRepresentation.setGroups(groups);
            }
            userResource.update(userRepresentation);
            userResource.joinGroup(groupId);
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
        if (userRepresentation.getAttributes() == null || !userRepresentation.getAttributes().containsKey(ENTITY)) {
            userRepresentation.singleAttribute(ENTITY, entityName);
        } else {
            List<String> entities = userRepresentation.getAttributes().getOrDefault(ENTITY, Collections.emptyList());
            if (!entities.contains(entityName)) {
                entities.add(entityName);
            }
        }
        addAttributeIfNotExists(userRepresentation, EMAIL, email);
        addAttributeIfNotExists(userRepresentation, MOBILE_NUMBER, mobile);
    }

    private void addAttributeIfNotExists(UserRepresentation userRepresentation, String key, String value) {
        if (userRepresentation.getAttributes() == null || !userRepresentation.getAttributes().containsKey(key)) {
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

    @Override
    public String getServiceName() {
        return SUNBIRD_KEYCLOAK_SERVICE_NAME;
    }

    @Override
    public ComponentHealthInfo getHealthInfo() {
        if (authenticationEnabled) {
            try {
                return new ComponentHealthInfo(getServiceName(), keycloak.serverInfo().getInfo().getSystemInfo().getUptimeMillis() > 0);
            } catch (Exception e) {
                return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, e.getMessage());
            }
        } else {
            return new ComponentHealthInfo(getServiceName(), true, "AUTHENTICATION_ENABLED", "false");
        }

    }
}
