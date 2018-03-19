package io.opensaber.registry.service;

import java.io.IOException;
import java.util.List;

import org.apache.jena.rdf.model.Model;

import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.InvalidTypeException;
import io.opensaber.registry.exception.RecordNotFoundException;

public interface RegistryService {
	
	public List getEntityList();
	
	public String addEntity(Model rdfModel) throws DuplicateRecordException, InvalidTypeException, EncryptionException;
	
	public boolean updateEntity(Model entity, String rootNodeLabel) throws RecordNotFoundException, InvalidTypeException, EncryptionException;
	
	public org.eclipse.rdf4j.model.Model getEntityById(String id) throws RecordNotFoundException, EncryptionException;
	
	public boolean deleteEntity(Object entity);

	public String frameEntity(org.eclipse.rdf4j.model.Model entityModel) throws IOException;

}
