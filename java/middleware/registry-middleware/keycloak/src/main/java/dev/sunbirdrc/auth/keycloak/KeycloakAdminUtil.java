package dev.sunbirdrc.auth.keycloak;

import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.identity_providers.pojos.*;
import dev.sunbirdrc.pojos.HealthIndicator;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
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

import java.util.*;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_KEYCLOAK_SERVICE_NAME;


public class KeycloakAdminUtil implements IdentityManager {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminUtil.class);
    private static final String EMAIL = "email_id";
    private static final String ENTITY = "entity";
    private static final String MOBILE_NUMBER = "mobile_number";
    private static final String PASSWORD = "password";
    private final Keycloak keycloak;

    private final IdentityProviderConfiguration providerConfiguration;
    public KeycloakAdminUtil(IdentityProviderConfiguration identityProviderConfiguration) {
        this.providerConfiguration = identityProviderConfiguration;
        this.keycloak = buildKeycloak(identityProviderConfiguration);
    }

    private Keycloak buildKeycloak(IdentityProviderConfiguration configuration) {
        return KeycloakBuilder.builder()
                .serverUrl(configuration.getUrl())
                .realm(configuration.getRealm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(configuration.getClientId())
                .clientSecret(configuration.getClientSecret())
                .resteasyClient(
                        new ResteasyClientBuilder()
                                .connectionPoolSize(configuration.getHttpMaxConnections()).build()
                )
                .build();
    }

    @Override
    public String createUser(CreateUserRequest createUserRequest) throws IdentityException {
        logger.info("Creating user with mobile_number : " + createUserRequest.getUserName());
        String groupId = createOrUpdateRealmGroup(createUserRequest.getEntity());
        UserRepresentation newUser = createUserRepresentation(createUserRequest);
        UsersResource usersResource = keycloak.realm(providerConfiguration.getRealm()).users();
        try (Response response = usersResource.create(newUser)) {
            if (response.getStatus() == 201) {
                logger.info("Response |  Status: {} | Status Info: {}", response.getStatus(), response.getStatusInfo());
                logger.info("User ID path" + response.getLocation().getPath());
                String userID = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                logger.info("User ID : " + userID);
                if (!providerConfiguration.getUserActions().isEmpty()) {
                    usersResource.get(userID).executeActionsEmail(providerConfiguration.getUserActions());
                }
                return userID;
            } else if (response.getStatus() == 409) {
                logger.info("UserID: {} exists", createUserRequest.getUserName());
                return updateExistingUserAttributes(createUserRequest, groupId);
            } else if (response.getStatus() == 500) {
                throw new IdentityException("Keycloak user creation error");
            } else {
                throw new IdentityException("Username already invited / registered");
            }
        }
    }

    private String createOrUpdateRealmGroup(String entityName) {
        RoleRepresentation roleRepresentation = createOrGetRealmRole(entityName);
        GroupsResource groupsResource = keycloak.realm(providerConfiguration.getRealm()).groups();
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
        RolesResource rolesResource = keycloak.realm(providerConfiguration.getRealm()).roles();
        try {
             return rolesResource.get(entityName).toRepresentation();
        } catch (NotFoundException ex) {
            logger.error("Role {} not found. Creating role {}", entityName, entityName);
            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setName(entityName);
            rolesResource.create(roleRepresentation);
        } catch (Exception e){
            logger.error("Role creation exception", e);
        }
        return rolesResource.get(entityName).toRepresentation();
    }

//    private String updateExistingUserAttributes(String entityName, String userName, String email, String mobile,
    private String updateExistingUserAttributes(CreateUserRequest createUserRequest, String groupId) throws IdentityException {
        Optional<UserResource> userRepresentationOptional = getUserByUsername(createUserRequest.getUserName());
        if (userRepresentationOptional.isPresent()) {
            UserResource userResource = userRepresentationOptional.get();
            UserRepresentation userRepresentation = userResource.toRepresentation();
            updateUserAttributes(createUserRequest, userRepresentation);
            List<String> groups = userRepresentation.getGroups();
            if (groups == null) {
                groups = new ArrayList<>();
            }
            if (!groups.contains(createUserRequest.getEntity())) {
                groups.add(createUserRequest.getEntity());
                userRepresentation.setGroups(groups);
            }
            userResource.update(userRepresentation);
            userResource.joinGroup(groupId);
            return userRepresentation.getId();
        } else {
            logger.error("Failed fetching user by username: {}", createUserRequest.getUserName());
            throw new IdentityException("Creating user failed");
        }
    }

    private UserRepresentation createUserRepresentation(CreateUserRequest createUserRequest) {
        UserRepresentation newUser = new UserRepresentation();
        newUser.setEnabled(true);
        newUser.setUsername(createUserRequest.getUserName());
        if (!Objects.equals(createUserRequest.getPassword(), "") || providerConfiguration.setDefaultPassword()) {
            CredentialRepresentation passwordCredential = new CredentialRepresentation();
            if (!Objects.equals(createUserRequest.getPassword(), "")) {
                passwordCredential.setValue(createUserRequest.getPassword());
                passwordCredential.setTemporary(false);
            } else {
                passwordCredential.setValue(providerConfiguration.getDefaultPassword());
                passwordCredential.setTemporary(true);
            }
            passwordCredential.setType(PASSWORD);
            newUser.setCredentials(Collections.singletonList(passwordCredential));
        }
        newUser.setGroups(Collections.singletonList(createUserRequest.getEntity()));
        newUser.setEmail(createUserRequest.getEmail());
        newUser.singleAttribute(MOBILE_NUMBER, createUserRequest.getMobile());
        newUser.singleAttribute(EMAIL, createUserRequest.getEmail());
        newUser.singleAttribute(ENTITY, createUserRequest.getEntity());
        return newUser;
    }

    private void updateUserAttributes(CreateUserRequest createUserRequest, UserRepresentation userRepresentation) {
        if (userRepresentation.getAttributes() == null || !userRepresentation.getAttributes().containsKey(ENTITY)) {
            userRepresentation.singleAttribute(ENTITY, createUserRequest.getEntity());
        } else {
            List<String> entities = userRepresentation.getAttributes().getOrDefault(ENTITY, Collections.emptyList());
            if (!entities.contains(createUserRequest.getEntity())) {
                entities.add(createUserRequest.getEntity());
            }
        }
        addAttributeIfNotExists(userRepresentation, EMAIL, createUserRequest.getEmail());
        addAttributeIfNotExists(userRepresentation, MOBILE_NUMBER, createUserRequest.getMobile());
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
        List<UserRepresentation> users = keycloak.realm(providerConfiguration.getRealm()).users().search(username);
        if (users.size() > 0) {
            return Optional.of(keycloak.realm(providerConfiguration.getRealm()).users().get(users.get(0).getId()));
        }
        return Optional.empty();
    }

    private void addUserToGroup(String groupName, UserRepresentation user) {
        keycloak.realm(providerConfiguration.getRealm()).groups().groups().stream()
                .filter(g -> g.getName().equals(groupName)).findFirst()
                .ifPresent(g -> keycloak.realm(providerConfiguration.getRealm()).users().get(user.getId()).joinGroup(g.getId()));
    }

    @Override
    public String getServiceName() {
        return SUNBIRD_KEYCLOAK_SERVICE_NAME;
    }

    @Override
    public ComponentHealthInfo getHealthInfo() {
        if (providerConfiguration.isAuthenticationEnabled()) {
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
