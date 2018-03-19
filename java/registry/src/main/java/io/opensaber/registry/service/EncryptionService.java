package io.opensaber.registry.service;

import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.springframework.http.ResponseEntity;

import io.opensaber.registry.exception.EncryptionException;

public interface EncryptionService {

	public ResponseEntity<String> encrypt(Object propertyValue) throws EncryptionException;
	
	public ResponseEntity<String> decrypt(Object propertyValue) throws EncryptionException;	
	
	public boolean encryptionRequired(VertexProperty<Object>  property) throws EncryptionException;
	
}
