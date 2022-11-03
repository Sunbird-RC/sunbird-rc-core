package dev.sunbirdrc.registry.service;

import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.registry.exception.EncryptionException;

import java.util.Map;

public interface EncryptionService extends HealthIndicator {

	public String encrypt(Object propertyValue) throws EncryptionException;

	public String decrypt(Object propertyValue) throws EncryptionException;

	public Map<String, Object> encrypt(Map<String, Object> propertyValue) throws EncryptionException;

	public Map<String, Object> decrypt(Map<String, Object> propertyValue) throws EncryptionException;

}
