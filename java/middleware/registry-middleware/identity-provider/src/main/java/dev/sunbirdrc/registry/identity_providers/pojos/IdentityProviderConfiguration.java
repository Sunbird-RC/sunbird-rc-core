package dev.sunbirdrc.registry.identity_providers.pojos;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class IdentityProviderConfiguration {
	@Value("${identity.provider-name}")
	private String provider;
	@Value("${identity.url}")
	private String url;
	@Value("${identity.create-user-path}")
	private String createUserPath;
	@Value("${identity.realm}")
	private String realm;
	@Value("${identity.admin-client-id}")
	private String clientId;
	@Value("${identity.admin-client-secret}")
	private String clientSecret;
	@Value("${identity.set-default-password}")
	private Boolean setDefaultPassword;
	@Value("${identity.default-password}")
	private String defaultPassword;
	@Value("${identity.user-actions}")
	private List<String> userActions;

	@Value("${http.max-connections:5}")
	private int httpMaxConnections;

	@Value("${authentication.enabled:true}")
	boolean authenticationEnabled;

	public Boolean setDefaultPassword() {
		return setDefaultPassword;
	}

	public String getCreateUserUrl() {
		return (url + createUserPath).replaceAll("(?<!:)//", "/");
	}
}
