package io.opensaber.registry.service.impl;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.InvalidTypeException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.util.GraphDBFactory;
import io.opensaber.utils.converters.RDF2Graph;
import io.opensaber.registry.middleware.util.Constants;


/**
 * 
 * @author jyotsna
 *
 */
@Component
public class RegistryServiceImpl implements RegistryService{

	@Autowired
	RegistryDao registryDao;

	@Autowired
	private Environment environment;

	@Override
	public List getEntityList(){
		return registryDao.getEntityList();
	}

	@Override
	public boolean addEntity(Model rdfModel) throws DuplicateRecordException, InvalidTypeException {
		Graph graph = GraphDBFactory.getEmptyGraph();
		StmtIterator iterator = rdfModel.listStatements();
		boolean rootSubjectFound = false;
		String label = null;
		while (iterator.hasNext()) {
			Statement rdfStatement = iterator.nextStatement();
			if (!rootSubjectFound) {
				String type = environment.getProperty(Constants.SUBJECT_LABEL_TYPE);
				label = RDF2Graph.getRootSubjectLabel(rdfStatement, type);
				if (label != null) {
					rootSubjectFound = true;
				}
			}
			graph = RDF2Graph.convertJenaRDFStatement2Graph(rdfStatement, graph);
		}
		if (label == null) {
			throw new InvalidTypeException(Constants.INVALID_TYPE_MESSAGE);
		}

		return registryDao.addEntity(graph, label);
	}

	@Override
	public boolean updateEntity(Model entity){
		String label = "";
		TinkerGraph tinkerGraph = TinkerGraph.open();
		return registryDao.updateEntity(tinkerGraph,label);
	}

	@Override
	public Graph getEntityById(String id) throws RecordNotFoundException{
		return registryDao.getEntityById(id);
	}

	@Override
	public boolean deleteEntity(Object entity){
		return registryDao.deleteEntity(entity);
	}
	

}
