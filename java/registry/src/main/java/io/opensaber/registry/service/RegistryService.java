package io.opensaber.registry.service;

import java.util.List;

import io.opensaber.registry.exception.DuplicateRecordException;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryService {
	
	public List getEntityList();
	
	public boolean addEntity(Object entity) throws NullPointerException, DuplicateRecordException;
	
	public boolean updateEntity(Object entity);
	
	public Object getEntityById(Object entity);
	
	public boolean deleteEntity(Object entity);

}
