package io.opensaber.registry.service;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;

import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.InvalidTypeException;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryService {
	
	public List getEntityList();
	
	public boolean addEntity(Model entity) throws DuplicateRecordException, InvalidTypeException;
	
	public boolean updateEntity(Model entity);
	
	public Object getEntityById(Object entity);
	
	public boolean deleteEntity(Object entity);

}
