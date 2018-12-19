package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jsonldjava.core.JsonLdError;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.exception.*;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.util.ReadConfigurator;
import org.apache.jena.rdf.model.Model;

import java.io.IOException;
import java.util.List;

public interface RegistryService {

	public List getEntityList();

	public String addEntity(Model rdfModel, String subject, String property)
			throws DuplicateRecordException, EntityCreationException, EncryptionException, AuditFailedException,
			MultipleEntityException, RecordNotFoundException;

	public String addEntity(Model rdfModel, String dataObject, String subject, String property)
			throws DuplicateRecordException, EntityCreationException, EncryptionException, AuditFailedException,
			MultipleEntityException, RecordNotFoundException, IOException, SignatureException.UnreachableException,
			JsonLdError, SignatureException.CreationException;

	public boolean updateEntity(Model entity) throws RecordNotFoundException, EntityCreationException,
            EncryptionException, AuditFailedException, MultipleEntityException, SignatureException.UnreachableException,
            IOException, SignatureException.CreationException, UpdateException;

	public Model getEntityById(String id, boolean includeSignatures)
			throws RecordNotFoundException, EncryptionException, AuditFailedException;

	public HealthCheckResponse health() throws Exception;

	public String frameEntity(Model entityModel) throws IOException, MultipleEntityException, EntityCreationException;

	public boolean deleteEntityById(String id) throws AuditFailedException, RecordNotFoundException;

	public String getEntityFramedById(String id, boolean includeSignatures) throws RecordNotFoundException,
			EncryptionException, AuditFailedException, IOException, MultipleEntityException, EntityCreationException;

	public void setDatabaseProvider(DatabaseProvider databaseProvider);

	public String addEntity(String shardId, String jsonString) throws Exception;

	JsonNode getEntity(String id, ReadConfigurator configurator);
}
