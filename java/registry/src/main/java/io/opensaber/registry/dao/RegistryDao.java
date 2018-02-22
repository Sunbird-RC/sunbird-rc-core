package io.opensaber.registry.dao;

import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Graph;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.RecordNotFoundException;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryDao {
	
	public List getEntityList();
	
	public boolean addEntity(Graph entity,String label) throws DuplicateRecordException;
	
	public boolean updateEntity(Graph entity,String label);
	
	public boolean deleteEntity(Object entity);

	public Graph getEntityById(String label) throws RecordNotFoundException;

}
