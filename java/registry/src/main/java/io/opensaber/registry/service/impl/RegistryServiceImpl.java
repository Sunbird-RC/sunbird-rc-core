package io.opensaber.registry.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import io.opensaber.registry.util.RDFUtil;
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
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import io.opensaber.converters.JenaRDF4J;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.InvalidTypeException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.util.GraphDBFactory;
import io.opensaber.utils.converters.RDF2Graph;

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
	public void addEntity(Model rdfModel) throws DuplicateRecordException, InvalidTypeException {
		try {
			Graph graph = GraphDBFactory.getEmptyGraph();
			RDFUtil.updateIdForBlankNode(rdfModel);

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
				org.eclipse.rdf4j.model.Statement rdf4jStatement = JenaRDF4J.asrdf4jStatement(rdfStatement);
				graph = RDF2Graph.convertRDFStatement2Graph(rdf4jStatement, graph);
			}

			if (label == null) {
				throw new InvalidTypeException(Constants.INVALID_TYPE_MESSAGE);
			}
			registryDao.addEntity(graph, label);
		} catch (DuplicateRecordException | InvalidTypeException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.error("Exception when creating entity: ", ex);
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
	public org.eclipse.rdf4j.model.Model getEntityById(String label) throws RecordNotFoundException{
		Graph graph = registryDao.getEntityById(label);
		org.eclipse.rdf4j.model.Model model = RDF2Graph.convertGraph2RDFModel(graph, label);
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
		ClassPathResource res = new ClassPathResource("frame.json");
		File file = res.getFile();
		String fileString = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
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
