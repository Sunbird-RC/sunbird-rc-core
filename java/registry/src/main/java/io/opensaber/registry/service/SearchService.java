package io.opensaber.registry.service;

import org.apache.jena.rdf.model.Model;

import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.exception.TypeNotProvidedException;


public interface SearchService {
	
	public org.eclipse.rdf4j.model.Model search(Model model) throws AuditFailedException, 
	EncryptionException, RecordNotFoundException, TypeNotProvidedException;

}
