package io.opensaber.registry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.pojos.Filter;
import io.opensaber.pojos.FilterOperators;
import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.dao.SearchDaoImpl;
import io.opensaber.registry.dao.ValueType;
import io.opensaber.registry.middleware.util.JSONUtil;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.service.ISearchService;
import io.opensaber.registry.sink.OSGraph;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.sink.shard.ShardManager;
import io.opensaber.registry.util.DefinitionsManager;
import io.opensaber.registry.util.RecordIdentifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
	private Shard shard;

	@Value("${database.uuidPropertyName}")
	public String uuidPropertyName;
	
	@Value("${search.offset}")
	private int offset;
	
	@Value("${search.limit}")
	private int limit;
	
    private SearchQuery getSearchQuery(JsonNode inputQueryNode) {
        String rootLabel = inputQueryNode.fieldNames().next();

        SearchQuery searchQuery = new SearchQuery(rootLabel, offset, limit);
        List<Filter> filterList = new ArrayList<>();
        JsonNode rootNode = inputQueryNode.get(rootLabel);
        if (rootLabel != null && !rootLabel.isEmpty()) {
            addToFilterList(null, rootNode, filterList);
        }
        // populates limit & offset
        try {
            searchQuery.setLimit(inputQueryNode.get("limit").asInt());
            searchQuery.setOffset(inputQueryNode.get("offset").asInt());
        } catch (Exception e) {
            logger.error("Populates SearchQuery for limit/offset: {}", e.getMessage());
        }

        searchQuery.setFilters(filterList);
        return searchQuery;
    }

	/**
	 * For a given path filter, iterate through the fields given and set the filterList
	 * @param path
	 * @param inputQueryNode
	 * @return
	 */
    private void addToFilterList(String path, JsonNode inputQueryNode, List<Filter> filterList) {
        Iterator<Map.Entry<String, JsonNode>> searchFields = inputQueryNode.fields();
     
        // Iterate and get the fields.
        while (searchFields.hasNext()) {
            Map.Entry<String, JsonNode> entry = searchFields.next();
            String property = entry.getKey();
            JsonNode entryVal = entry.getValue();
            if (entryVal.isObject() && (entryVal.fields().hasNext())) {
                Map.Entry<String, JsonNode> entryValMap = entryVal.fields().next();
                String operatorStr = entryValMap.getKey();
                
                if (entryValMap.getValue().isObject()) {
                    addToFilterList(entry.getKey(), entryVal, filterList);
                } else {
                    Object value = null;
                    if (entryValMap.getValue().isArray()) {
                        value = getObjects(entryValMap.getValue());

                    } else if (entryValMap.getValue().isValueNode()) {
                        value = ValueType.getValue(entryValMap.getValue());
                    }
                    FilterOperators operator = FilterOperators.get(operatorStr);
                    if(operator == null)
                        throw new IllegalArgumentException("Search query cannot perform without operator!");

                    Filter filter = new Filter(property, operator, value);
                    filter.setPath(path);
                    filterList.add(filter);
                }
            } else {
                 throw new IllegalArgumentException("Search query is invalid!");
            }
        }
    }
    /**
     * Return all values
     * 
     * @param node
     * @return
     */
    private List<Object> getObjects(JsonNode node) {
            
        List<Object> rangeValues = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            JsonNode entryVal = node.get(i);
            if (entryVal.isValueNode())
                rangeValues.add(ValueType.getValue(entryVal));
        }
        return rangeValues;
    }

	@Override
	public JsonNode search(JsonNode inputQueryNode) {
		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		SearchQuery searchQuery = getSearchQuery(inputQueryNode);

		// Now, search across all shards and return the results.
		for (DBConnectionInfo dbConnection : dbConnectionInfoMgr.getConnectionInfo()) {

			// TODO: parallel search.
			shardManager.activateShard(dbConnection.getShardId());
			IRegistryDao registryDao = new RegistryDaoImpl(shard.getDatabaseProvider(), definitionsManager, uuidPropertyName);
			SearchDaoImpl searchDao = new SearchDaoImpl(registryDao);
			try (OSGraph osGraph = shard.getDatabaseProvider().getOSGraph()) {
				Graph graph = osGraph.getGraphStore();
				try (Transaction tx = shard.getDatabaseProvider().startTransaction(graph)) {
					ArrayNode oneShardResult = (ArrayNode) searchDao.search(graph, searchQuery);
					for (JsonNode jsonNode: oneShardResult) {
						if (!shard.getShardLabel().isEmpty()) {
							// Replace osid with shard details
							String prefix = shard.getShardLabel() + RecordIdentifier.getSeparator();
							JSONUtil.addPrefix((ObjectNode) jsonNode, prefix, new ArrayList<>(Arrays.asList(uuidPropertyName)));
						}

						result.add(jsonNode);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return result;
	}
}
