package dev.sunbirdrc.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class PropertiesValueMapper {
    @Value("${keycloak.server.url}")
    private String keycloakServerUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.confidential.client.id}")
    private String confidentialClientId;
    @Value("${keycloak.public.client.id}")
    private String publicClientId;
    @Value("${keycloak.client.secret}")
    private String clientSecret;
    @Value("${keycloak.username}")
    private String userName;
    @Value("${keycloak.password}")
    private String password;
    @Value("${keycloak.token.url}")
    private String keyCloakTokenUrl;
    @Value("${otp.mail.verification.subject}")
    private String otpMailVerificationSubject;
    @Value("${otp.mail.verification.personal.name}")
    private String otpMailVerificationPersonalName;
    @Value("${otp.mail.verification.from.address}")
    private String otpMailVerificationFromAddress;
    @Value("${otp.ttl.duration}")
    private Long otpTtlDuration;
    @Value("${otp.time.unit}")
    private String otpTimeUnit;
    @Value("${custom.user.login.url}")
    private String customUserLoginUrl;
    @Value("${bulk.user.size.limit}")
    private Integer bulkUserSizeLimit;
    @Value("${custom.user.creation.subject}")
    private String customUserCreationSubject;
    @Value("${custom.user.creation.personal.name}")
    private String customUserCreationPersonalName;
    @Value("${custom.user.creation.from.address}")
    private String customUserCreationFromAddress;
    @Value("${custom.redis.host}")
    private String customRedisHost;
    @Value("${custom.redis.port}")
    private Integer customRedisPort;
    @Value("${custom.user.credential.secret.key}")
    private String customUserCredentialSecretKey;
    @Value("${custom.user.cipher.provider.algorithm}")
    private String customUserCipherProviderAlgorithm;
}
