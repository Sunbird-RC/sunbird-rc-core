package io.opensaber.registry.service;

import java.util.List;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryService {
	
	public List getEntityList();
	
	public boolean addEntity(Object entity);
	
	public boolean updateEntity(Object entity);
	
	public Object getEntityById(Object entity);
	
	public boolean deleteEntity(Object entity);

}
