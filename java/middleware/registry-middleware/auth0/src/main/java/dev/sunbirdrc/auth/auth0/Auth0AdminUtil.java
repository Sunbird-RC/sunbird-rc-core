package dev.sunbirdrc.auth.auth0;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.RolesFilter;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.json.mgmt.permissions.Permission;
import com.auth0.json.mgmt.resourceserver.ResourceServer;
import com.auth0.json.mgmt.resourceserver.Scope;
import com.auth0.json.mgmt.roles.Role;
import com.auth0.json.mgmt.roles.RolesPage;
import com.auth0.json.mgmt.users.User;
import com.auth0.json.mgmt.users.UsersPage;
import com.auth0.net.TokenRequest;
import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.identity_providers.pojos.IdentityProviderConfiguration;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.sunbirdrc.registry.identity_providers.pojos.CreateUserRequest;
import dev.sunbirdrc.registry.identity_providers.pojos.IdentityException;
import dev.sunbirdrc.registry.identity_providers.pojos.IdentityManager;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;


public class Auth0AdminUtil implements IdentityManager {
	private static final Logger logger = LoggerFactory.getLogger(Auth0AdminUtil.class);
	private static final String EMAIL = "email_id";
	private static final String ENTITY = "entity";
	private static final String MOBILE_NUMBER = "mobile_number";
	private static final String PASSWORD = "password";
	public static final String SUNBIRD_AUTH0_SERVICE_NAME = "sunbird.auth0.service";
	private final IdentityProviderConfiguration identityProviderConfiguration;

	@Value("${auth0.resource.server.id}")
	private String resourceServerId;

	@Value("${auth0.resource.user.connection:Username-Password-Authentication}")
	private String userConnection;


	public Auth0AdminUtil(IdentityProviderConfiguration identityProviderConfiguration) {
		this.identityProviderConfiguration = identityProviderConfiguration;
	}


	@Override
	public String createUser(CreateUserRequest createUserRequest) throws IdentityException {
		logger.info("Creating user with mobile_number : " + createUserRequest.getUserName());
		try {
			String token = getToken();
			ManagementAPI mgmt = ManagementAPI.newBuilder(identityProviderConfiguration.getRealm(), token).build();
			Optional<String> roleOptional = createOrGetRole(mgmt, createUserRequest);
			if (roleOptional.isPresent()) {
				Optional<String> userOptional = createOrGetUser(mgmt, createUserRequest, roleOptional.get());
				if (userOptional.isPresent()) {
					return userOptional.get();
				} else {
					throw new IdentityException("Creating user failed");
				}
			}
		} catch (Exception e) {
			logger.error("Error creating user in auth0", e);
			throw new IdentityException(String.format("Auth0 user creation error %s", e.getMessage()));
		}
		return "";
	}

	private Optional<String> createOrGetUser(ManagementAPI mgmt, CreateUserRequest createUserRequest, String roleId) throws Auth0Exception {
		try {
			User userObject = createUserObject(createUserRequest);
			User userRepresentation = mgmt.users().create(userObject).execute().getBody();
			assignUserRole(mgmt, roleId, userRepresentation.getId());
			return Optional.ofNullable(userRepresentation.getId());
		} catch (Exception e) {
			logger.error("User creation exception, {}", e.getMessage());
		}
		UsersPage usersPage = searchUser(mgmt, createUserRequest);
		if (usersPage.getItems().size() > 0) {
			User existingUser = usersPage.getItems().get(0);
			assignUserRole(mgmt, roleId, existingUser.getId());
			return Optional.ofNullable(existingUser.getId());
		}
		return Optional.empty();
	}

	private void assignUserRole(ManagementAPI mgmt, String roleId, String userId) throws Auth0Exception {
		mgmt.roles().assignUsers(roleId, Collections.singletonList(userId)).execute();
	}

	private UsersPage searchUser(ManagementAPI mgmt, CreateUserRequest createUserRequest) throws Auth0Exception {
		UserFilter userFilter = new UserFilter();
		userFilter.withQuery(createUserRequest.getUserName());
		return mgmt.users().list(userFilter).execute().getBody();
	}

	private Optional<String> createOrGetRole(ManagementAPI mgmt, CreateUserRequest createUserRequest) throws Auth0Exception {
		String role = createUserRequest.getEntity();
		try {
			Role roleRepresentation = createRole(mgmt, role);
			addPermission(mgmt, role, roleRepresentation);
			return Optional.ofNullable(roleRepresentation.getId());
		} catch (Exception e) {
			logger.error("Role creation exception, {}", e.getMessage());
		}
		RolesPage rolesPage = searchExistingRoles(mgmt, role);
		if (rolesPage.getItems().size() > 0) {
			Role existingRoleRepresentation = rolesPage.getItems().get(0);
			addPermission(mgmt, role, existingRoleRepresentation);
			return Optional.ofNullable(existingRoleRepresentation.getId());
		}
		return Optional.empty();
	}

	private void addScopeToResourceServer(ManagementAPI mgmt, String role) {
		try {
			ResourceServer resourceServer = new ResourceServer();
			resourceServer.setScopes(Collections.singletonList(new Scope(role)));
			mgmt.resourceServers().update(resourceServerId, resourceServer).execute();
		} catch (Exception e) {
			logger.error("Error adding scopes to resource server, {}", e.getMessage());
		}
	}

	private RolesPage searchExistingRoles(ManagementAPI mgmt, String role) throws Auth0Exception {
		RolesFilter filter = new RolesFilter();
		filter.withName(role);
		return mgmt.roles().list(filter).execute().getBody();
	}

	private Role createRole(ManagementAPI mgmt, String role) throws Auth0Exception {
		Role roleRepresentation = new Role();
		roleRepresentation.setName(role);
		roleRepresentation = mgmt.roles().create(roleRepresentation).execute().getBody();
		return roleRepresentation;
	}

	private void addPermission(ManagementAPI mgmt, String role, Role roleRepresentation) throws Auth0Exception {
		addScopeToResourceServer(mgmt, role);
		Permission permission = new Permission();
		permission.setName(role);
		permission.setResourceServerId(resourceServerId);
		mgmt.roles().addPermissions(roleRepresentation.getId(), Collections.singletonList(permission)).execute();
	}

	private User createUserObject(CreateUserRequest createUserRequest) {
		User user = new User();
		user.setEmail(createUserRequest.getEmail());
//		user.setPhoneNumber(createUserRequest.getMobile());
		user.setName(createUserRequest.getUserName());
		Map<String, Object> userMetadata = new HashMap<>();
		userMetadata.put(ENTITY, Collections.singletonList(createUserRequest.getEntity()));
		userMetadata.put(EMAIL, createUserRequest.getEmail());
		userMetadata.put(MOBILE_NUMBER, createUserRequest.getMobile());
		user.setUserMetadata(userMetadata);
		user.setConnection("Username-Password-Authentication");
		if (!Strings.isBlank(createUserRequest.getPassword())) {
			user.setPassword(createUserRequest.getPassword().toCharArray());
		}
		return user;
	}

	//TODO: cache token
	public String getToken() throws Auth0Exception {
		AuthAPI authAPI = AuthAPI.newBuilder(identityProviderConfiguration.getRealm(),
				identityProviderConfiguration.getClientId(), identityProviderConfiguration.getClientSecret()).build();
		TokenRequest tokenRequest = authAPI.requestToken(identityProviderConfiguration.getUrl());
		TokenHolder holder = tokenRequest.execute().getBody();
		return holder.getAccessToken();

	}

	@Override
	public String getServiceName() {
		return SUNBIRD_AUTH0_SERVICE_NAME;
	}

	@Override
	public ComponentHealthInfo getHealthInfo() {
		if (identityProviderConfiguration.isAuthenticationEnabled()) {
			try {
				//TODO: check auth0 status
				return new ComponentHealthInfo(getServiceName(), true);
			} catch (Exception e) {
				return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, e.getMessage());
			}
		} else {
			return new ComponentHealthInfo(getServiceName(), true, "AUTHENTICATION_ENABLED", "false");
		}
	}
}
