package io.opensaber.registry.service;

import io.opensaber.registry.exception.EncryptionException;

public interface EncryptionService {
	
	public String encrypt(Object propertyValue) throws EncryptionException;
	
	public String decrypt(Object propertyValue) throws EncryptionException;
	
	public boolean isEncryptionServiceUp();

}
