package io.opensaber.registry.service;

import java.io.IOException;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.opensaber.pojos.APIMessage;
import io.opensaber.pojos.AuditRecord;
import io.opensaber.pojos.Filter;
import io.opensaber.pojos.FilterOperators;
import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.dao.SearchDaoImpl;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.registry.util.DefinitionsManager;
import io.opensaber.registry.util.RecordIdentifier;
/**
 * This class provides native search which hits the native database
 * Hence, this have performance in-efficiency on search operations
 *
 */
@Component
public class NativeSearchService implements ISearchService {

	private static Logger logger = LoggerFactory.getLogger(NativeSearchService.class);

	@Autowired
	private DBConnectionInfoMgr dbConnectionInfoMgr;

	@Autowired
	private DefinitionsManager definitionsManager;

	@Autowired
	private ShardManager shardManager;

	@Autowired
	private IAuditService auditService;

	@Autowired
	private APIMessage apiMessage;

	@Value("${database.uuidPropertyName}")
	public String uuidPropertyName;

	@Value("${search.offset}")
	private int offset;
	
	@Value("${search.limit}")
	private int limit;

    @Value("${audit.enabled}")
    private boolean auditEnabled;
    
    @Value("${audit.frame.suffix}")
    private String auditSuffix;

	@Override
	public JsonNode search(JsonNode inputQueryNode) throws IOException {
		
		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		SearchQuery searchQuery = getSearchQuery(inputQueryNode, offset, limit);

		if(searchQuery.getFilters().size() == 1 && searchQuery.getFilters().get(0).getOperator() == FilterOperators.queryString)
            throw new IllegalArgumentException("free-text queries not supported for native search!");

		Filter uuidFilter = getUUIDFilter(searchQuery, uuidPropertyName);
		boolean isSpecificSearch = (uuidFilter != null);

		boolean continueSearch = true;
		// Now, search across all shards and return the results.
		for (DBConnectionInfo dbConnection : dbConnectionInfoMgr.getConnectionInfo()) {

			if (continueSearch) {
				if (isSpecificSearch) {
					RecordIdentifier recordIdentifier = RecordIdentifier.parse(uuidFilter.getValue().toString());

					if (!uuidFilter.getValue().equals(recordIdentifier.getUuid())) {
						// value is not just uuid and so trim out
						uuidFilter.setValue(recordIdentifier.getUuid());
					}
				}

				// TODO: parallel search.
				List<Integer> transaction = new LinkedList<>();
				Shard shard = shardManager.activateShard(dbConnection.getShardId());
				IRegistryDao registryDao = new RegistryDaoImpl(shard.getDatabaseProvider(), definitionsManager, uuidPropertyName);
				SearchDaoImpl searchDao = new SearchDaoImpl(registryDao);
				try (OSGraph osGraph = shard.getDatabaseProvider().getOSGraph()) {
					Graph graph = osGraph.getGraphStore();
					try (Transaction tx = shard.getDatabaseProvider().startTransaction(graph)) {
						ObjectNode shardResult = (ObjectNode) searchDao.search(graph, searchQuery);
						if (!shard.getShardLabel().isEmpty()) {
							// Replace osid with shard details
							String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
							JSONUtil.addPrefix((ObjectNode) shardResult, prefix, new ArrayList<>(Arrays.asList(uuidPropertyName)));
						}

						result.add(shardResult);
						transaction.add(tx.hashCode());
					}
				} catch (Exception e) {
					logger.error("search operation failed: {}", e);
				} finally {
					continueSearch = !isSpecificSearch;
				}
				
		        //if Audit enabled in configuration yml file
		        if(auditEnabled) {
		        	String operation = Constants.AUDIT_ACTION_SEARCH_OP;
		        	String action = Constants.AUDIT_ACTION_SEARCH;
		        	if(searchQuery.getEntityTypes().get(0).contains(auditSuffix)) {
		        		operation = Constants.AUDIT_ACTION_AUDIT_OP;
		        		action = Constants.AUDIT_ACTION_AUDIT;
		        	}

		        	AuditRecord auditRecord = auditService.createAuditRecord(apiMessage.getUserID(), action, "", transaction);
			        auditRecord.setAuditInfo(auditService.createAuditInfo(operation, action, null, inputQueryNode, searchQuery.getEntityTypes()));
		        	auditService.doAudit(auditRecord, inputQueryNode, searchQuery.getEntityTypes(), null, shard);
		        }
		 	}
		}
		
		return buildResultNode(searchQuery, result);
	}
	
	/**
	 * combines all the nodes for an entity
	 * @param entity
	 * @param allShardResult
	 * @return
	 */
	private ArrayNode getEntityAttibute(String entity, ArrayNode allShardResult) {
		ArrayNode resultArray = JsonNodeFactory.instance.arrayNode();
		for (int i = 0; i < allShardResult.size(); i++) {
			resultArray.addAll((ArrayNode) allShardResult.get(i).get(entity));
		}
		return resultArray;
	}
	/**
	 * Builds result node from given array of shard nodes 
	 * @param searchQuery
	 * @param allShardResult
	 * @return
	 */
	private JsonNode buildResultNode(SearchQuery searchQuery, ArrayNode allShardResult) {
		ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
		for (String entity : searchQuery.getEntityTypes()) {
			ArrayNode entityResult = getEntityAttibute(entity, allShardResult);
			resultNode.set(entity, entityResult);
		}
		return resultNode;
	}
}
