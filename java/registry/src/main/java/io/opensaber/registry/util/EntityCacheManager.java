package io.opensaber.registry.util;

import io.opensaber.registry.dao.TPGraphMain;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EntityCacheManager {

	private static Logger logger = LoggerFactory.getLogger(EntityCacheManager.class);

	@Autowired
	private TPGraphMain tpGraphMain;
	@Autowired
	private DBProviderFactory dbProviderFactory;
	private Set<String> defintionNames;
	private List<DBConnectionInfo> dbConnectionInfoList;
	private Map<String, List<String>> shardUUIDSMap;

	@Autowired
	public EntityCacheManager(DefinitionsManager definitionsManager, DBConnectionInfoMgr dbConnectionInfoMgr) {
		this.defintionNames = definitionsManager.getAllKnownDefinitions();
		this.dbConnectionInfoList = dbConnectionInfoMgr.getConnectionInfo();
		shardUUIDSMap = new ConcurrentHashMap<>();
	}

	/**
	 * Retrive Ids for all vertices of each shard and creates the mapping
	 * between the shard and its vertices. Loads at application start up
	 */
	public void loadShardUUIDS() {
		dbConnectionInfoList.forEach(dbConnectionInfo -> {
			DatabaseProvider dbProvider = dbProviderFactory.getInstance(dbConnectionInfo);
			Graph graph = dbProvider.getGraphStore();
			List<String> uuids = new ArrayList<>();
			logger.info("defintionNames for getting UUIDS: " + defintionNames);
			uuids.addAll(tpGraphMain.getUUIDs(graph, defintionNames));
			logger.info("UUIDS for definationNames: " + tpGraphMain.getUUIDs(graph, defintionNames));
			shardUUIDSMap.put(dbConnectionInfo.getShardId(), uuids);
			logger.info("cache added shard: " + dbConnectionInfo.getShardId() + " with " + uuids.size() + " uuids");
		});

	}

	public Map<String, List<String>> getShardUUIDs() {
		return shardUUIDSMap;
	}

}
