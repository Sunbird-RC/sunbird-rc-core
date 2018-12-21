package io.opensaber.registry.util;

import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EntityCacheManager {

    private static Logger logger = LoggerFactory.getLogger(EntityCacheManager.class);

    @Autowired
    private Environment environment;
    @Autowired
    private RegistryDaoImpl tpGraphMain;
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
        logger.info("defintionNames for getting UUIDS: " + defintionNames);
    }

    /**
     * Retrive Ids for all vertices of each shard and creates the mapping
     * between the shard and its vertices. Loads at application start up
     */
    public void loadShardUUIDS() {
        String shardProperty = environment.getProperty("database.shardProperty");
        if (shardProperty.compareToIgnoreCase(Constants.NONE_STR) != 0) {
            dbConnectionInfoList.forEach(dbConnectionInfo -> {
                DatabaseProvider dbProvider = dbProviderFactory.getInstance(dbConnectionInfo);
                try {
                    try (OSGraph osGraph = dbProvider.getOSGraph()) {
                        Graph graph = osGraph.getGraphStore();

                        List<String> uuids = tpGraphMain.getUUIDs(graph, defintionNames);
                        if (!uuids.isEmpty()) {
                            logger.info("UUIDS for definitionNames: " + uuids);
                            shardUUIDSMap.put(dbConnectionInfo.getShardId(), uuids);
                        }
                        logger.info("Shard: " + dbConnectionInfo.getShardId() + " added cache with " + uuids.size() + " uuids");
                    }
                } catch (Exception e) {
                    logger.info("Can't load shard uuids. Cache won't be warmed.");
                }
            });
        } else {
            logger.info("No shards configured. Cache not needed.");
        }
    }

    public Map<String, List<String>> getShardUUIDs() {
        return shardUUIDSMap;
    }

}
