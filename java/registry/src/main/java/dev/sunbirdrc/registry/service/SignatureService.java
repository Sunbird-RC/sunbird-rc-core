package dev.sunbirdrc.registry.service;

import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.registry.exception.SignatureException;

public interface SignatureService extends HealthIndicator {

	Object sign(Object propertyValue)
			throws SignatureException.UnreachableException, SignatureException.CreationException;

	boolean verify(Object propertyValue)
			throws SignatureException.UnreachableException, SignatureException.VerificationException;

	String getKey(String keyId) throws SignatureException.UnreachableException, SignatureException.KeyNotFoundException;

}
