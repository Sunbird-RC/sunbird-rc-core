package dev.sunbirdrc.registry.service;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.sunbirdrc.registry.dao.IRegistryDao;
import dev.sunbirdrc.registry.dao.RegistryDaoImpl;

import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.sink.DatabaseProvider;
import dev.sunbirdrc.registry.sink.OSGraph;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.ReadConfigurator;
import dev.sunbirdrc.registry.util.RecordIdentifier;

/**
 * This class provides native search which hits the native database
 * Hence, this have performance in-efficiency on search operations
 *
 */
@Component
public class NativeReadService implements IReadService {

	private static Logger logger = LoggerFactory.getLogger(NativeReadService.class);

	@Autowired
	private IDefinitionsManager definitionsManager;

	@Autowired
	private IAuditService auditService;

	@Value("${database.uuidPropertyName}")
	public String uuidPropertyName;

    @Value("${audit.enabled}")
    private boolean auditEnabled;

	/**
	 * This method interacts with the native db and reads the record
	 *
	 * @param id           - osid
	 * @param entityType
	 * @param configurator
	 * @return
	 * @throws Exception
	 */
	@Override
	public JsonNode getEntity(Shard shard, String userId, String id, String entityType, ReadConfigurator configurator) throws Exception {
		DatabaseProvider dbProvider = shard.getDatabaseProvider();
		IRegistryDao registryDao = new RegistryDaoImpl(dbProvider, definitionsManager, uuidPropertyName);
		try (OSGraph osGraph = dbProvider.getOSGraph()) {
			Graph graph = osGraph.getGraphStore();
			Transaction tx = dbProvider.startTransaction(graph);
			JsonNode result = registryDao.getEntity(graph, entityType, id, configurator);

			if (!shard.getShardLabel().isEmpty()) {
				// Replace osid with shard details
				String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
				JSONUtil.addPrefix((ObjectNode) result, prefix, new ArrayList<String>(Arrays.asList(uuidPropertyName)));
			}

			dbProvider.commitTransaction(graph, tx);
			
			auditService.auditRead(auditService.createAuditRecord(userId, id, tx, entityType), shard);

			return result;
		}
	}

}
