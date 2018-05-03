package io.opensaber.registry.authorization;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.keycloak.RSATokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyCloakServiceImpl {

	private static Logger logger = LoggerFactory.getLogger(KeyCloakServiceImpl.class);

public String verifyToken(String accessToken) throws VerificationException, Exception {
	String userId = "";
    try {
        PublicKey publicKey = toPublicKey(System.getenv("sunbird_sso_publickey"));
        AccessToken token = RSATokenVerifier.verifyToken(accessToken, publicKey,
                System.getenv("sunbird_sso_url") + "realms/" + System.getenv("sunbird_sso_realm"), true, true);
        userId = token.getSubject();
        logger.debug("Authentication token \n TokenId: {} \t isActive: {} \t isExpired: {} \t", token.getId(), token.isActive(), token.isExpired());
    } catch (VerificationException e) {
        logger.error("Invalid Auth token or Environment Variable !" + e);
        throw new VerificationException();
    } catch (Exception e) {
        logger.error("Authentication Failed ! " + e);
        throw new Exception();
    }
    return userId;
	}


private PublicKey toPublicKey(String publicKeyString) {
	try {
		byte[] publicBytes = Base64.getDecoder().decode(publicKeyString);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePublic(keySpec);
	} catch (Exception e) {
		return null;
	}
}

}
