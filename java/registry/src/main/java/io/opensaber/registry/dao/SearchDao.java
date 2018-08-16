package io.opensaber.registry.dao;

import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.RecordNotFoundException;

import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Graph;

public interface SearchDao {
	
	public Map<String,Graph> search(SearchQuery searchQuery) throws AuditFailedException, EncryptionException, RecordNotFoundException;

}
