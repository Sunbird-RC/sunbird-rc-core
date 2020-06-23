package io.opensaber.registry.service.impl;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.Definition;
import io.opensaber.registry.util.DefinitionsManager;
import io.opensaber.registry.util.EntityParenter;

/**
 * 
 * Save audit details to DB system
 *
 */
@Component
public class AuditDBWriter {
	private static Logger logger = LoggerFactory.getLogger(AuditDBWriter.class);
	 
    @Value("${persistence.commit.enabled:true}")
    private boolean commitEnabled;

    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;
    
    @Autowired
    private DefinitionsManager definitionsManager;
    
    @Autowired
    private EntityParenter entityParenter;
    
	public String auditToDB(Shard shard, JsonNode rootNode, String entityType) throws AuditFailedException {

    	String entityId = "auditPlaceholderId";
	 	Transaction tx = null;
        DatabaseProvider dbProvider = shard.getDatabaseProvider();
        IRegistryDao registryDao = new RegistryDaoImpl(dbProvider, definitionsManager, uuidPropertyName);
        try (OSGraph osGraph = dbProvider.getOSGraph()) {
            Graph graph = osGraph.getGraphStore();
            tx = dbProvider.startTransaction(graph);
            entityId = registryDao.addEntity(graph, rootNode);
            if (commitEnabled) {
                dbProvider.commitTransaction(graph, tx);
            }

            logger.debug("Audit added : " + entityId);
        } catch (Exception e) {
            logger.error("Audit failed : {}" + e);

            throw new AuditFailedException("Audit failed : " + e.getMessage());
        } finally {
            tx.close();
        }
        // Add indices: executes only once.
        String shardId = shard.getShardId();
        Vertex parentVertex = entityParenter.getKnownParentVertex(entityType, shardId);
        Definition definition = definitionsManager.getDefinition(entityType);
        entityParenter.ensureIndexExists(dbProvider, parentVertex, definition, shardId);
        return entityId;
	}
}
