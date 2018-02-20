package io.opensaber.registry.service;

import java.util.List;

import org.apache.jena.rdf.model.Model;

import io.opensaber.registry.exception.DuplicateRecordException;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryService {
	
	public List getEntityList();
	
	public boolean addEntity(Model entity) throws DuplicateRecordException;
	
	public boolean updateEntity(Model entity);
	
	public Object getEntityById(Object entity);
	
	public boolean deleteEntity(Object entity);

}
