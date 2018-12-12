package io.opensaber.registry.service;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;

import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.MultipleEntityException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.exception.TypeNotProvidedException;
import io.opensaber.registry.sink.DatabaseProvider;

public interface SearchService {

	public org.eclipse.rdf4j.model.Model search(Model model)
			throws AuditFailedException, EncryptionException, RecordNotFoundException, TypeNotProvidedException;

	public String searchFramed(Model model) throws AuditFailedException, EncryptionException, RecordNotFoundException,
			TypeNotProvidedException, IOException, MultipleEntityException, EntityCreationException;
	
	public void setDatabaseProvider(DatabaseProvider databaseProvider);
}
