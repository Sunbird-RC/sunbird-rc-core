package io.opensaber.registry.service.impl;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.util.GraphDBFactory;
import io.opensaber.utils.converters.RDF2Graph;
import io.opensaber.registry.middleware.util.Constants;

@Component
public class RegistryServiceImpl implements RegistryService {

	private static Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

	@Autowired
	RegistryDao registryDao;

	@Autowired
	private Environment environment;

	@Override
	public List getEntityList(){
		return registryDao.getEntityList();
	}

	@Override
	public boolean addEntity(Model entity) throws DuplicateRecordException{
  try {
		Graph graph = GraphDBFactory.getEmptyGraph();
		Model rdfModel = (Model)entity;
		StmtIterator iterator = rdfModel.listStatements();
		boolean rootSubjectFound = false;
		String label = null;
		while(iterator.hasNext()){
			Statement rdfStatement = iterator.nextStatement();
			String type = environment.getProperty(Constants.SUBJECT_LABEL_TYPE);
			String subjectValue = rdfStatement.getSubject().toString();
			String predicate = rdfStatement.getPredicate().toString();
			if(!rootSubjectFound && predicate.equals(RDF.TYPE.toString())){
				RDFNode object = rdfStatement.getObject();
				if(object.isURIResource()){
					if(object.toString().equals(type)){
						label = subjectValue;
						rootSubjectFound = true;
						logger.info("Printing label:"+label);
						}
					}
				}
				graph = RDF2Graph.convertRDFStatement2Graph(rdfStatement, graph);
			}
			return registryDao.addEntity(graph, label);

		} catch (Exception ex) {
			logger.error("Error when creating entity in RegistryServiceImpl : ", ex);
			throw ex;
		}
	}

	@Override
	public boolean updateEntity(Model entity){
		String label = "";
		TinkerGraph tinkerGraph = TinkerGraph.open();
		return registryDao.updateEntity(tinkerGraph,label);
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
