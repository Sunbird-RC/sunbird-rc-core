package io.opensaber.registry.dao;

import java.util.List;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryDao {
	
	public List getEntityList();
	
	public boolean addEntity(Object entity);
	
	public boolean updateEntity(Object entity);
	
	public Object getEntityById(Object entity);
	
	public boolean deleteEntity(Object entity);

}
