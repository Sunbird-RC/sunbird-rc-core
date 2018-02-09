package io.opensaber.registry.dao;

import java.util.List;

import io.opensaber.registry.exception.DuplicateRecordException;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryDao {
	
	public List getEntityList();
	
	public boolean addEntity(Object entity,String label) throws DuplicateRecordException, NullPointerException;
	
	public boolean updateEntity(Object entity);
	
	public Object getEntityById(Object entity);
	
	public boolean deleteEntity(Object entity);

}
