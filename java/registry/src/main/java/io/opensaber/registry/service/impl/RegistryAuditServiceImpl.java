package io.opensaber.registry.service.impl;

import io.opensaber.converters.JenaRDF4J;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.exception.*;
import io.opensaber.registry.frame.FrameEntity;
import io.opensaber.registry.service.RegistryAuditService;
import io.opensaber.utils.converters.RDF2Graph;
import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

@Component
public class RegistryAuditServiceImpl implements RegistryAuditService {

    private static Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

    @Autowired
    private RegistryDao registryDao;

    @Value("${audit.frame.file}")
    private String auditFrameFile;

    @Autowired
    private FrameEntity frameEntity;

    @Override
    public String frameAuditEntity(org.eclipse.rdf4j.model.Model entityModel) throws IOException {
        Model jenaEntityModel = JenaRDF4J.asJenaModel(entityModel);
        logger.debug("RegistryServiceImpl : jenaEntityModel for audit-framing: {} ", jenaEntityModel);
        DatasetGraph g = DatasetFactory.create(jenaEntityModel).asDatasetGraph();
        JsonLDWriteContext ctx = new JsonLDWriteContext();
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(auditFrameFile);
        String fileString = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
        ctx.setFrame(fileString);
        WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(org.apache.jena.riot.RDFFormat.JSONLD_FRAME_FLAT);
        PrefixMap pm = RiotLib.prefixMap(g);
        String base = null;
        StringWriter sWriterJena = new StringWriter();
        w.write(sWriterJena, g, pm, base, ctx);
        String jenaJSON = sWriterJena.toString();
        logger.debug("RegistryServiceImpl : jenaJSON for audit-framing: {}", jenaJSON);
        return jenaJSON;
    }

    @Override
    public org.eclipse.rdf4j.model.Model getAuditNode(String id) throws IOException, NoSuchElementException,
            RecordNotFoundException, EncryptionException, AuditFailedException {
        String label = id + "-AUDIT";
        Graph graph = registryDao.getEntityById(label, false);
        org.eclipse.rdf4j.model.Model model = RDF2Graph.convertGraph2RDFModel(graph, label);
        logger.debug("RegistryServiceImpl : Audit Model : " + model);
        return model;
    }

    @Override
    public String getAuditNodeFramed(String id) throws IOException, NoSuchElementException, RecordNotFoundException,
            EncryptionException, AuditFailedException, IOException, MultipleEntityException, EntityCreationException {
        String label = id + "-AUDIT";
        Graph graph = registryDao.getEntityById(label, false);
        org.eclipse.rdf4j.model.Model model = RDF2Graph.convertGraph2RDFModel(graph, label);
        logger.debug("RegistryServiceImpl : Audit Model : " + model);
        return frameEntity.getContent(model);
    }
}
