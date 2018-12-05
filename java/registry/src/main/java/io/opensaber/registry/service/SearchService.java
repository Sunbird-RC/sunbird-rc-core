package io.opensaber.registry.service;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;

import io.opensaber.registry.exception.*;

public interface SearchService {

	public org.eclipse.rdf4j.model.Model search(Model model)
			throws AuditFailedException, EncryptionException, RecordNotFoundException, TypeNotProvidedException;

	public String searchFramed(Model model) throws AuditFailedException, EncryptionException, RecordNotFoundException,
			TypeNotProvidedException, IOException, MultipleEntityException, EntityCreationException;

}
