package io.opensaber.registry.dao;

import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Graph;

import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.RecordNotFoundException;

public interface SearchDao {

	public Map<String, Graph> search(SearchQuery searchQuery)
			throws AuditFailedException, EncryptionException, RecordNotFoundException;

}
