package io.opensaber.registry.service;

import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.springframework.http.ResponseEntity;

import io.opensaber.registry.exception.EncryptionException;

public interface EncryptionService {

	public String encrypt(Object propertyValue) throws EncryptionException;
	
	public String decrypt(Object propertyValue) throws EncryptionException;	
	
	public boolean isEncryptable(String  propertyKey) throws EncryptionException;
	
	public boolean isDecryptable(String tailPropertyKey) throws EncryptionException;
	
}
