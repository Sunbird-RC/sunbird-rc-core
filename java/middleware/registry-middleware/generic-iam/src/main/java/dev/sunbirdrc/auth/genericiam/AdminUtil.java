package dev.sunbirdrc.auth.genericiam;

import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.registry.identity_providers.pojos.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Objects;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;


public class AdminUtil implements IdentityManager {
	public static final String SUNBIRD_GENERIC_IAM_SERVICE_NAME = "sunbird.generic-iam.service";
	private static final Logger logger = LoggerFactory.getLogger(AdminUtil.class);
	private final IdentityProviderConfiguration identityProviderConfiguration;

	private final RestTemplate restTemplate;

	private final String iamServiceURL;

	public AdminUtil(IdentityProviderConfiguration identityProviderConfiguration) {
		this.identityProviderConfiguration = identityProviderConfiguration;
		this.restTemplate = new RestTemplate();
		this.iamServiceURL = System.getenv()
				.getOrDefault("sunbird_sso_url", "http://localhost:3990/fusionauth/api/v1/user");
	}


	@Override
	public String createUser(CreateUserRequest createUserRequest) throws IdentityException {
		logger.info("Creating user with mobile_number : " + createUserRequest.getUserName());
		ResponseEntity<CreateUserResponse> response = this.restTemplate.postForEntity(this.iamServiceURL,
				createUserRequest, CreateUserResponse.class);
		if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null &&
				!StringUtils.isEmpty(response.getBody().getUserId())) {
			return response.getBody().getUserId();
		}
		logger.error("Error creating user in IAM service");
		throw new IdentityException(String.format("User creation error %s", createUserRequest));
	}


	@Override
	public String getServiceName() {
		return SUNBIRD_GENERIC_IAM_SERVICE_NAME;
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
