package io.opensaber.registry.dao;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Graph;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.RecordNotFoundException;

public interface RegistryDao {
	
	public List getEntityList();
	
	public boolean addEntity(Graph entity, String label) throws DuplicateRecordException, NoSuchElementException, EncryptionException;
	
	public boolean updateEntity(Graph entityForUpdate, String rootNodeLabel, String methodOrigin) throws RecordNotFoundException, NoSuchElementException, EncryptionException;
	
	public boolean deleteEntity(Object entity);

	public Graph getEntityById(String label) throws RecordNotFoundException, NoSuchElementException, EncryptionException;

}
