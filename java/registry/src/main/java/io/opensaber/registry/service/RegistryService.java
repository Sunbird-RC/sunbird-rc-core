package io.opensaber.registry.service;

import java.io.IOException;
import java.util.List;

import org.apache.jena.rdf.model.Model;

import com.github.jsonldjava.core.JsonLdError;

import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.MultipleEntityException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.exception.SignatureException;
import io.opensaber.registry.middleware.MiddlewareHaltException;
import io.opensaber.validators.exception.RDFValidationException;
import io.opensaber.validators.exception.ValidationFactoryException;

public interface RegistryService {

	public List getEntityList();

	public String addEntity(Model rdfModel, String subject, String property)
			throws DuplicateRecordException, EntityCreationException, EncryptionException, AuditFailedException,
			MultipleEntityException, RecordNotFoundException;

	public String addEntity(Model rdfModel, String dataObject, String subject, String property)
            throws DuplicateRecordException, EntityCreationException, EncryptionException, AuditFailedException,
            MultipleEntityException, RecordNotFoundException, IOException, SignatureException.UnreachableException, JsonLdError, SignatureException.CreationException, RDFValidationException, MiddlewareHaltException, ValidationFactoryException;

	public boolean updateEntity(Model entity) throws RecordNotFoundException, EntityCreationException,
			EncryptionException, AuditFailedException, MultipleEntityException, SignatureException.UnreachableException,
			IOException, SignatureException.CreationException, RDFValidationException, MiddlewareHaltException, ValidationFactoryException;

	public Model getEntityById(String id, boolean includeSignatures)
			throws RecordNotFoundException, EncryptionException, AuditFailedException;

	public HealthCheckResponse health() throws Exception;

	public String frameEntity(Model entityModel) throws IOException, MultipleEntityException, EntityCreationException;

	public boolean deleteEntityById(String id) throws AuditFailedException, RecordNotFoundException;

	public String getEntityFramedById(String id, boolean includeSignatures) throws RecordNotFoundException,
			EncryptionException, AuditFailedException, IOException, MultipleEntityException, EntityCreationException;

}
