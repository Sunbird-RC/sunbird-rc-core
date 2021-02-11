package io.opensaber.registry.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.opensaber.pojos.AuditRecord;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.DefinitionsManager;
import io.opensaber.registry.util.ReadConfigurator;
import io.opensaber.registry.util.RecordIdentifier;

/**
 * This class provides native search which hits the native database
 * Hence, this have performance in-efficiency on search operations
 *
 */
@Component
public class NativeReadService implements IReadService {

	private static Logger logger = LoggerFactory.getLogger(NativeReadService.class);

	@Autowired
	private DefinitionsManager definitionsManager;

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
			
	        //if Audit enabled in configuration yml file
	        if(auditEnabled) {
				List<Integer> transaction = new LinkedList<>(Arrays.asList(tx.hashCode()));
				List<String> entityTypes = new LinkedList<>(Arrays.asList(entityType));
				
		        AuditRecord auditRecord = auditService.createAuditRecord(userId, Constants.AUDIT_ACTION_READ, id, transaction);
		        auditRecord.setAuditInfo(auditService.createAuditInfo(Constants.AUDIT_ACTION_READ_OP, Constants.AUDIT_ACTION_READ, null,null, entityTypes));
				auditService.doAudit(auditRecord, null, entityTypes, id, shard);
	        }
	 
			return result;
		}
	}

}
