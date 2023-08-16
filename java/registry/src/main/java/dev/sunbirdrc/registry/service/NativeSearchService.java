package dev.sunbirdrc.registry.service;

import java.io.IOException;
import java.util.*;

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

import dev.sunbirdrc.pojos.APIMessage;
import dev.sunbirdrc.pojos.AuditRecord;
import dev.sunbirdrc.pojos.Filter;
import dev.sunbirdrc.pojos.FilterOperators;
import dev.sunbirdrc.pojos.SearchQuery;
import dev.sunbirdrc.registry.dao.IRegistryDao;
import dev.sunbirdrc.registry.dao.RegistryDaoImpl;
import dev.sunbirdrc.registry.dao.SearchDaoImpl;
import dev.sunbirdrc.registry.middleware.util.JSONUtil;
import dev.sunbirdrc.registry.model.DBConnectionInfo;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.sink.OSGraph;
import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.sink.shard.ShardManager;
import dev.sunbirdrc.registry.util.IDefinitionsManager;
import dev.sunbirdrc.registry.util.RecordIdentifier;
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
	private IDefinitionsManager definitionsManager;

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

	@Value("${search.expandInternal}")
	private boolean expandInternal;

	@Value("${search.removeNonPublicFieldsForNativeSearch:false}")
	private boolean removeNonPublicFieldsForNativeSearch;

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
				List<Object> transaction = new LinkedList<>();

				Shard shard = shardManager.activateShard(dbConnection.getShardId());
				IRegistryDao registryDao = new RegistryDaoImpl(shard.getDatabaseProvider(), definitionsManager, uuidPropertyName);
				SearchDaoImpl searchDao = new SearchDaoImpl(registryDao);
				try (OSGraph osGraph = shard.getDatabaseProvider().getOSGraph()) {
					Graph graph = osGraph.getGraphStore();
					try (Transaction tx = shard.getDatabaseProvider().startTransaction(graph)) {
						ObjectNode shardResult = (ObjectNode) searchDao.search(graph, searchQuery, expandInternal);
						if (!shard.getShardLabel().isEmpty()) {
							// Replace osid with shard details
							String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
							JSONUtil.addPrefix((ObjectNode) shardResult, prefix, new ArrayList<>(Arrays.asList(uuidPropertyName)));
						}
						result = removeNonPublicFields(searchQuery, shardResult);
						if (tx != null) {
							transaction.add(tx.hashCode());
						}
					}
				} catch (Exception e) {
					logger.error("search operation failed: {}", e);
				} finally {
					continueSearch = !isSpecificSearch;
				}
				try {
					auditService.auditNativeSearch(
							new AuditRecord()
									.setUserId(apiMessage.getUserID())
									.setTransactionId(transaction),
							shard, searchQuery.getEntityTypes(), inputQueryNode);
				} catch (Exception e) {
					logger.error("Exception while auditing " + e);
				}

		 	}
		}

		return buildResultNode(searchQuery, result);
	}

	private ArrayNode removeNonPublicFields(SearchQuery searchQuery, ObjectNode shardResult) throws Exception {
		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		for(String entityType: searchQuery.getEntityTypes()) {
			ArrayNode arrayNode = (ArrayNode) shardResult.get(entityType);
			if (removeNonPublicFieldsForNativeSearch) {
				for(JsonNode node : arrayNode) {
					result.add(JSONUtil.removeNodesByPath(node, definitionsManager.getExcludingFieldsForEntity(entityType)));
				}
			} else {
				result = arrayNode;
			}
		}
		return result;
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
			resultNode.set(entity, allShardResult);
		}
		return resultNode;
	}
}
