package io.opensaber.registry.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.opensaber.converters.JenaRDF4J;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.InvalidTypeException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.util.GraphDBFactory;
import io.opensaber.utils.converters.RDF2Graph;

import static org.apache.tinkerpop.gremlin.structure.io.IoCore.graphson;

@Component
public class RegistryServiceImpl implements RegistryService {

	private static Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

	@Autowired
	private RegistryDao registryDao;

	@Autowired
	private Environment environment;

	@Override
	public List getEntityList(){
		return registryDao.getEntityList();
	}

	@Override
	public String addEntity(Model rdfModel) throws DuplicateRecordException, InvalidTypeException, EncryptionException{
		try {
			Graph graph = GraphDBFactory.getEmptyGraph();
			StmtIterator iterator = rdfModel.listStatements();
			boolean rootSubjectFound = false;
			boolean rootNodeBlank = false;
			String label = null;

			while (iterator.hasNext()) {
				Statement rdfStatement = iterator.nextStatement();
				if (!rootSubjectFound) {
					String type = environment.getProperty(Constants.SUBJECT_LABEL_TYPE);
					label = RDF2Graph.getRootSubjectLabel(rdfStatement, type);
					if (label != null) {
						rootSubjectFound = true;
					}
					if(rdfStatement.getSubject().isAnon() && rdfStatement.getSubject().getURI() == null) {
						rootNodeBlank = true;
					}
				}
				org.eclipse.rdf4j.model.Statement rdf4jStatement = JenaRDF4J.asrdf4jStatement(rdfStatement);
				graph = RDF2Graph.convertRDFStatement2Graph(rdf4jStatement, graph);
			}

			if (label == null) {
				throw new InvalidTypeException(Constants.INVALID_TYPE_MESSAGE);
			}

			// Append _: to the root node label to create the entity as Apache Jena removes the _: for the root node label
			// if it is a blank node
			return registryDao.addEntity(graph, rootNodeBlank ? String.format("_:%s", label) : label);
			
		} catch (DuplicateRecordException | InvalidTypeException | EncryptionException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.error("Exception when creating entity: ", ex);
			throw ex;
		}
	}

	@Override
	public boolean updateEntity(Model entity, String rootNodeLabel) throws RecordNotFoundException, InvalidTypeException, EncryptionException {
		Graph graph = GraphDBFactory.getEmptyGraph();

		StmtIterator iterator = entity.listStatements();
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
			org.eclipse.rdf4j.model.Statement rdf4jStatement = JenaRDF4J.asrdf4jStatement(rdfStatement);
			graph = RDF2Graph.convertRDFStatement2Graph(rdf4jStatement, graph);
		}

		if (label == null) {
			throw new InvalidTypeException(Constants.INVALID_TYPE_MESSAGE);
		}

		return registryDao.updateEntity(graph, rootNodeLabel,"addOrUpdate");
	}

	@Override
	public org.eclipse.rdf4j.model.Model getEntityById(String label) throws RecordNotFoundException, EncryptionException{
		Graph graph = registryDao.getEntityById(label);
        try {
            graph.io(graphson()).writeGraph("graph.json");
        } catch (IOException e) {
            e.printStackTrace();
        };
		org.eclipse.rdf4j.model.Model model = RDF2Graph.convertGraph2RDFModel(graph, label);
        for (org.eclipse.rdf4j.model.Statement statement: model) {
            logger.info("STATEMENT "+statement);
            Value value = statement.getObject();
            if (value instanceof Literal) {
                Literal literal = (Literal) value;
                System.out.println("datatype: " + literal.getDatatype());
            }
        }
		logger.info("ENTITY in Service "+model);
		return model;
	}

	@Override
	public boolean deleteEntity(Object entity){
		return registryDao.deleteEntity(entity);
	}

	@Override
	public String frameEntity(org.eclipse.rdf4j.model.Model entityModel) throws IOException {
		Model jenaEntityModel = JenaRDF4J.asJenaModel(entityModel);
		DatasetGraph g = DatasetFactory.create(jenaEntityModel).asDatasetGraph();
		JsonLDWriteContext ctx = new JsonLDWriteContext();
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("frame.json");
		String fileString = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
		ctx.setFrame(fileString);
		WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(org.apache.jena.riot.RDFFormat.JSONLD_FRAME_FLAT) ;
		PrefixMap pm = RiotLib.prefixMap(g);
		String base = null;
		StringWriter sWriterJena = new StringWriter();
		w.write(sWriterJena, g, pm, base, ctx) ;
		String jenaJSON = sWriterJena.toString();
		return jenaJSON;
	}
	

}
