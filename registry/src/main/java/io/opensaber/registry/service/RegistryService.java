package io.opensaber.registry.service;

import java.util.List;

import io.opensaber.registry.model.dto.EntityDto;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryService {
	
	public List getEntityList();
	
	public boolean addEntity(Object entity);
	
	public boolean updateEntity(Object entity);
	
	public Object getEntityById(EntityDto entityDto);
	
	public boolean deleteEntity(EntityDto entityDto);

}
