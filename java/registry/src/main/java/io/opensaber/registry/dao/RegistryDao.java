package io.opensaber.registry.dao;

import java.util.List;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Graph;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.RecordNotFoundException;

public interface RegistryDao {
	
	public List getEntityList();
	
	public boolean addEntity(Graph entity,String label) throws DuplicateRecordException;
	
	public boolean updateEntity(Graph entityForUpdate, String rootNodeLabel) throws RecordNotFoundException;
	
	public boolean deleteEntity(Object entity);

	public Graph getEntityById(String Id) throws RecordNotFoundException;

}
