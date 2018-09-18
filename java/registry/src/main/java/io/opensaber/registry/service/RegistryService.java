package io.opensaber.registry.service;

import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.exception.*;
import org.apache.jena.rdf.model.Model;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

public interface RegistryService {
	
	public List getEntityList();
	
	//public String addEntity(Model rdfModel) throws DuplicateRecordException, EntityCreationException, EncryptionException, AuditFailedException, MultipleEntityException, RecordNotFoundException;
	
	public String addEntity(Model rdfModel, String subject, String property) throws DuplicateRecordException, EntityCreationException, 
	EncryptionException, AuditFailedException, MultipleEntityException, RecordNotFoundException;
	
	public boolean updateEntity(Model entity) throws RecordNotFoundException, EntityCreationException, EncryptionException, AuditFailedException, MultipleEntityException;

	public org.eclipse.rdf4j.model.Model getEntityById(String id, boolean includeSignatures) throws RecordNotFoundException, EncryptionException, AuditFailedException;

	//public boolean deleteEntity(Model rdfModel) throws AuditFailedException, RecordNotFoundException;

	public HealthCheckResponse health() throws Exception;

	public String frameEntity(org.eclipse.rdf4j.model.Model entityModel) throws IOException, MultipleEntityException, EntityCreationException;
	
	public String frameSearchEntity(org.eclipse.rdf4j.model.Model entityModel) throws IOException, MultipleEntityException, EntityCreationException;
	
	public String frameAuditEntity(org.eclipse.rdf4j.model.Model entityModel) throws IOException;

	public org.eclipse.rdf4j.model.Model getAuditNode(String id) throws IOException, NoSuchElementException, RecordNotFoundException,
	EncryptionException, AuditFailedException;

	public boolean deleteEntityById(String id) throws AuditFailedException, RecordNotFoundException;
}
