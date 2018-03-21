package io.opensaber.registry.service;

import java.io.IOException;
import java.util.List;

import io.opensaber.registry.exception.*;
import org.apache.jena.rdf.model.Model;

public interface RegistryService {
	
	public List getEntityList();
	
	public String addEntity(Model rdfModel) throws DuplicateRecordException, InvalidTypeException, EncryptionException, AuditFailedException;
	
	public boolean updateEntity(Model entity, String rootNodeLabel) throws RecordNotFoundException, InvalidTypeException, EncryptionException, AuditFailedException;
	
	public org.eclipse.rdf4j.model.Model getEntityById(String id) throws RecordNotFoundException, EncryptionException, AuditFailedException;
	
	public boolean deleteEntity(Object entity);

	public String frameEntity(org.eclipse.rdf4j.model.Model entityModel) throws IOException;

}
