package io.opensaber.registry.service.impl;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.util.GraphDBFactory;
import io.opensaber.utils.converters.RDF2Graph;


/**
 * 
 * @author jyotsna
 *
 */
@Component
public class RegistryServiceImpl implements RegistryService{
	
	@Autowired
	RegistryDao registryDao;
	
	@Override
	public List getEntityList(){
		return registryDao.getEntityList();
	}
	
	@Override
	public boolean addEntity(Object entity) throws NullPointerException, DuplicateRecordException{
		Graph graph = GraphDBFactory.getEmptyGraph();
		Model rdfModel = (Model)entity;
		StmtIterator iterator = rdfModel.listStatements();
		boolean firstSubjectFlag = true;
		String label = null;
		while(iterator.hasNext()){
			Statement rdfStatement = iterator.nextStatement();
			if(firstSubjectFlag){
				label = rdfStatement.getSubject().toString();
				System.out.println("Printing label:"+label);
				firstSubjectFlag = false;
			}
			graph = RDF2Graph.convertRDFStatement2Graph(rdfStatement, graph);
		}
		
		return registryDao.addEntity(graph,label);
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
