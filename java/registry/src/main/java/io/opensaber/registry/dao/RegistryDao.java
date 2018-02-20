package io.opensaber.registry.dao;

import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Graph;
import io.opensaber.registry.exception.DuplicateRecordException;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryDao {
	
	public List getEntityList();
	
	public boolean addEntity(Graph entity,String label) throws DuplicateRecordException;
	
	public boolean updateEntity(Graph entity,String label);
	
	public Object getEntityById(Object entity);
	
	public boolean deleteEntity(Object entity);

}
