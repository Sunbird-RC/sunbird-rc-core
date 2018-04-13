package io.opensaber.registry.service;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.exception.*;
import org.apache.jena.rdf.model.Model;
import org.springframework.boot.actuate.health.Health;

public interface RegistryService {
	
	public List getEntityList();
	
	public String addEntity(Model rdfModel) throws DuplicateRecordException, InvalidTypeException, EncryptionException, AuditFailedException, RecordNotFoundException;
	
	public boolean updateEntity(Model entity, String rootNodeLabel) throws RecordNotFoundException, InvalidTypeException, EncryptionException, AuditFailedException;
	
	public boolean upsertEntity(Model entity, String rootNodeLabel) throws RecordNotFoundException, InvalidTypeException, EncryptionException, AuditFailedException;
	
	public org.eclipse.rdf4j.model.Model getEntityById(String id) throws RecordNotFoundException, EncryptionException, AuditFailedException;
	
	public boolean deleteEntity(Object entity) throws AuditFailedException, RecordNotFoundException;

	public HealthCheckResponse health() throws Exception;

	public String frameEntity(org.eclipse.rdf4j.model.Model entityModel) throws IOException;
	
	public String frameAuditEntity(org.eclipse.rdf4j.model.Model entityModel) throws IOException;

	public org.eclipse.rdf4j.model.Model getAuditNode(String id) throws IOException, NoSuchElementException, RecordNotFoundException,
	EncryptionException, AuditFailedException;
}
