package io.opensaber.registry.service;

import io.opensaber.registry.exception.EncryptionException;

import java.util.Map;

public interface EncryptionService {

	public String encrypt(Object propertyValue) throws EncryptionException;

	public String decrypt(Object propertyValue) throws EncryptionException;

	public Map<String, Object> encrypt(Map<String, Object> propertyValue) throws EncryptionException;

	public Map<String, Object> decrypt(Map<String, Object> propertyValue) throws EncryptionException;

	public boolean isEncryptionServiceUp();

}
