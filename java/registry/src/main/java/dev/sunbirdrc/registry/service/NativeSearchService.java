package dev.sunbirdrc.registry.service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.node.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

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

import static dev.sunbirdrc.registry.middleware.util.Constants.ENTITY_LIST;
import static dev.sunbirdrc.registry.middleware.util.Constants.TOTAL_COUNT;

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
	@Value("${registry.expandReference}")
	private boolean expandReferenceObj;

	@Value("${search.removeNonPublicFieldsForNativeSearch:true}")
	private boolean removeNonPublicFieldsForNativeSearch;

	@Override
	public JsonNode search(JsonNode inputQueryNode, String userId) throws IOException {
		return search(inputQueryNode, userId, false);
	}

	public JsonNode search(JsonNode inputQueryNode, String userId, boolean skipRemoveNonPublicFields) throws IOException {

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
				IRegistryDao registryDao = new RegistryDaoImpl(shard.getDatabaseProvider(), definitionsManager, uuidPropertyName, expandReferenceObj);
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
						result.add(removeNonPublicFields(searchQuery, shardResult, skipRemoveNonPublicFields));
						if (tx != null) {
							transaction.add(tx.hashCode());
						}
					}
				} catch (Exception e) {
					logger.error("search operation failed: {}", ExceptionUtils.getStackTrace(e));
				} finally {
					continueSearch = !isSpecificSearch;
				}
				try {
					if(userId == null) userId = apiMessage.getUserID();
					auditService.auditNativeSearch(
							new AuditRecord()
									.setUserId(userId)
									.setTransactionId(transaction),
							shard, searchQuery.getEntityTypes(), inputQueryNode);
				} catch (Exception e) {
					logger.error("Exception while auditing: {}", ExceptionUtils.getStackTrace(e));
				}
			}
		}

		return buildResultNode(searchQuery, result);
	}

	private ObjectNode removeNonPublicFields(SearchQuery searchQuery, ObjectNode shardResult, boolean skipRemoveNonPublicFields) throws Exception {
		ObjectNode response = JsonNodeFactory.instance.objectNode();
		NumericNode count;
		for(String entityType: searchQuery.getEntityTypes()) {
			ObjectNode result = JsonNodeFactory.instance.objectNode();
			ArrayNode data = JsonNodeFactory.instance.arrayNode();
			ArrayNode arrayNode = (ArrayNode) (shardResult.get(entityType).get(ENTITY_LIST));
			count = (NumericNode) shardResult.get(entityType).get(TOTAL_COUNT);
			if (removeNonPublicFieldsForNativeSearch && !skipRemoveNonPublicFields) {
				for(JsonNode node : arrayNode) {
					data.add(JSONUtil.removeNodesByPath(node, definitionsManager.getExcludingFieldsForEntity(entityType)));
				}
			} else {
				data = arrayNode;
			}
			result.set(TOTAL_COUNT, count);
			result.set(ENTITY_LIST, data);
			response.set(entityType, result);
		}

		return response;
	}

	/**
	 * Builds result node from given array of shard nodes
	 * @param searchQuery
	 * @param allShardResult
	 * @return
	 */
	private JsonNode buildResultNode(SearchQuery searchQuery, ArrayNode allShardResult) throws IOException {
		ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
		for (String entity : searchQuery.getEntityTypes()) {
			List<JsonNode> entityResults = allShardResult.findValues(entity);
			ArrayNode data = JsonNodeFactory.instance.arrayNode();
			AtomicLong count = new AtomicLong(0L);
			ObjectNode entityResultsAggregate = JsonNodeFactory.instance.objectNode();
			entityResults.forEach(shardData -> {
				data.addAll((ArrayNode) shardData.get(ENTITY_LIST));
				count.addAndGet(shardData.get(TOTAL_COUNT).asLong());
			});
			entityResultsAggregate.set(TOTAL_COUNT, JsonNodeFactory.instance.numberNode(count.get()));
			entityResultsAggregate.set(ENTITY_LIST, data);
			resultNode.set(entity, entityResultsAggregate);
		}
		return resultNode;
	}
}
