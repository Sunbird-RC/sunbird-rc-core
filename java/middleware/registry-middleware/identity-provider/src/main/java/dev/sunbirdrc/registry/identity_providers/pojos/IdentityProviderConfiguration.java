package dev.sunbirdrc.registry.identity_providers.pojos;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class IdentityProviderConfiguration {
	@Value("${identity.provider}")
	private String provider;
	@Value("${identity.url}")
	private String url;
	@Value("${identity.realm}")
	private String realm;
	@Value("${identity.client_id}")
	private String clientId;
	@Value("${identity.client_secret}")
	private String clientSecret;
	@Value("${identity.set_default_password}")
	private Boolean setDefaultPassword;
	@Value("${identity.default_password}")
	private String defaultPassword;
	@Value("${identity.user_actions}")
	private List<String> userActions;

	@Value("${httpConnection.maxConnections:5}")
	private int httpMaxConnections;

	@Value("${authentication.enabled:true}")
	boolean authenticationEnabled;

	public Boolean setDefaultPassword() {
		return setDefaultPassword;
	}
}
