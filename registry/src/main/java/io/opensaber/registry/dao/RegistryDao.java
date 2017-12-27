package io.opensaber.registry.dao;

import java.util.List;

import io.opensaber.registry.model.Teacher;
import io.opensaber.registry.model.dto.EntityDto;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryDao {
	
	public List getEntityList();
	
	public boolean addEntity(Object entity);
	
	public boolean updateEntity(Object entity);
	
	public Object getEntityById(EntityDto entityDto);
	
	public boolean deleteEntity(EntityDto entityDto);

}
