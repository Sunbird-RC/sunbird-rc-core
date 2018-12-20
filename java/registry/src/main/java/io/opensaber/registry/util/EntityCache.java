package io.opensaber.registry.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EntityCache {

	private static Logger logger = LoggerFactory.getLogger(EntityCache.class);
	private final static String RECORD_NOT_FOUND = "Record not found";
	private Map<String, List<String>> recordShardMap;

	@Autowired
	public EntityCache(EntityCacheManager entityCacheManager) {
		this.recordShardMap = entityCacheManager.getShardUUIDs();
	}

	/**
	 * Provide shard identifier for a given record.
	 * 
	 * @param recordId
	 * @return
	 * @throws IOException
	 */

	public String getShard(String recordId) throws IOException {
		String shardId = "";
		for (Entry<String, List<String>> entry : recordShardMap.entrySet()) {
			if (entry.getValue().contains(recordId)) {
				shardId = entry.getKey();
				logger.info("Record " + recordId + " found a match in cache for shard " + shardId);
				break;
			}
		}
		if (shardId.isEmpty()) {
			logger.error("Record " + recordId + " not found in cache");
			throw new IOException(RECORD_NOT_FOUND);
		}
		return shardId;
	}

	/**
	 * Cache to add with a new shard record mapping
	 * 
	 * @param shardId
	 * @param recordId
	 * @return
	 */
	public boolean addEntity(String shardId, String recordId) {
		boolean added = false;
		if (recordShardMap.entrySet().contains(shardId)) {
			recordShardMap.get(shardId).add(recordId);
			logger.info("Existing shard id " + shardId + " added with record id " + recordId + " in cache");
			added = true;
		} else {
			List<String> recordIds = new ArrayList<>();
			recordIds.add(recordId);
			recordShardMap.put(shardId, recordIds);
			logger.info("Shard id " + shardId + " and record id " + recordId + " added in cache");
			added = true;
		}
		return added;
	}

}
