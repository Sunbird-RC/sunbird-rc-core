package io.opensaber.registry.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.service.RegistryService;

/**
 * 
 * @author jyotsna
 *
 */
@Service
public class RegistryServiceImpl implements RegistryService{
	
	@Autowired
	RegistryDao registryDao;
	
	@Override
	public List getEntityList(){
		return registryDao.getEntityList();
	}
	
	@Override
	public boolean addEntity(Object entity){
		return registryDao.addEntity(entity);
	}
	
	@Override
	public boolean updateEntity(Object entity){
		return registryDao.updateEntity(entity);
	}
	
	@Override
	public Object getEntityById(Object entity){
		return registryDao.getEntityById(entity);
	}
	
	@Override
	public boolean deleteEntity(Object entity){
		return registryDao.deleteEntity(entity);
	}

}
