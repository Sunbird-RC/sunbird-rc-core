package io.opensaber.registry.service;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.tinkerpop.gremlin.structure.Graph;

import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.InvalidTypeException;
import io.opensaber.registry.exception.RecordNotFoundException;

/**
 * 
 * @author jyotsna
 *
 */
public interface RegistryService {
	
	public List getEntityList();
	
	public boolean addEntity(Model entity) throws DuplicateRecordException, InvalidTypeException;
	
	public boolean updateEntity(Model entity);
	
	public Graph getEntityById(String id) throws RecordNotFoundException;
	
	public boolean deleteEntity(Object entity);

}
