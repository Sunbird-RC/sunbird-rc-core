package io.opensaber.registry.util;

import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("entityParenter")
public class EntityParenter {
    private static Logger logger = LoggerFactory.getLogger(EntityParenter.class);

    @Autowired
    private DBProviderFactory dbProviderFactory;

    @Autowired
    private RegistryDaoImpl tpGraphMain;

    private DefinitionsManager definitionsManager;
    private DBConnectionInfoMgr dbConnectionInfoMgr;

    private Set<String> defintionNames;
    private List<DBConnectionInfo> dbConnectionInfoList;
    /**
     * Holds information about a shard and a list of definitionParents
     */
    private HashMap<String, ShardParentInfoList> shardParentMap = new HashMap<>();

    @Autowired
    public EntityParenter(DefinitionsManager definitionsManager, DBConnectionInfoMgr dbConnectionInfoMgr) {
        this.definitionsManager = definitionsManager;
        this.dbConnectionInfoMgr = dbConnectionInfoMgr;

        defintionNames = this.definitionsManager.getAllKnownDefinitions();
        dbConnectionInfoList = this.dbConnectionInfoMgr.getConnectionInfo();
    }

    /**
     * Creates the parent vertex in all the shards for all default definitions
     *
     * @return
     */
    public Optional<String> ensureKnownParenters() {
        logger.info("Start - ensure parent node for defined schema");
        Optional<String> result;

        dbConnectionInfoList.forEach(dbConnectionInfo -> {
            logger.info("Starting to parents for {} definitions in shard {}", defintionNames.size(), dbConnectionInfo.getShardId());
            DatabaseProvider dbProvider = dbProviderFactory.getInstance(dbConnectionInfo);
            try {
                try (OSGraph osGraph = dbProvider.getOSGraph()) {
                    Graph graph = osGraph.getGraphStore();
                    List<ShardParentInfo> shardParentInfoList = new ArrayList<>();
                    try (Transaction tx = dbProvider.startTransaction(graph)) {

                        List<String> parentLabels = new ArrayList<>();
                        defintionNames.forEach(defintionName -> {
                            String parentLabel = ParentLabelGenerator.getLabel(defintionName);
                            parentLabels.add(parentLabel);
                            Vertex v = tpGraphMain.ensureParentVertex(graph, parentLabel);

                            ShardParentInfo shardParentInfo = new ShardParentInfo(defintionName, v);
                            shardParentInfo.setUuid(v.id().toString());
                            shardParentInfoList.add(shardParentInfo);
                        });

                        ShardParentInfoList valList = new ShardParentInfoList();
                        valList.setParentInfos(shardParentInfoList);

                        shardParentMap.put(dbConnectionInfo.getShardId(), valList);

                        dbProvider.commitTransaction(graph, tx);
                    }
                    logger.info("Ensured parents for {} definitions in shard {}", defintionNames.size(), dbConnectionInfo.getShardId());
                }
            } catch (Exception e) {
                logger.error("Can't ensure parents for definitions " + e);
            }
        });

        logger.info("End - ensure parent node for defined schema");
        result = Optional.empty();
        return result;
    }

    /**
     * Gets a known parent id
     *
     * @return
     */
    public String getKnownParentId(String definition, String shardId) {
        String id = "";
        ShardParentInfoList shardParentInfoList = shardParentMap.get(shardId);
        for (ShardParentInfo shardParentInfo : shardParentInfoList.getParentInfos()) {
            if (shardParentInfo.getName().compareToIgnoreCase(definition) == 0) {
                id = shardParentInfo.getUuid();
                break;
            }
        }
        return id;
    }

    /**
     * Gets a known parent vertex
     *
     * @return
     */
    public Vertex getKnownParentVertex(String definition, String shardId) {
        Vertex vertex = null;
        ShardParentInfoList shardParentInfoList = shardParentMap.get(shardId);
        for (ShardParentInfo shardParentInfo : shardParentInfoList.getParentInfos()) {
            if (shardParentInfo.getName().compareToIgnoreCase(definition) == 0) {
                vertex = shardParentInfo.getVertex();
                break;
            }
        }
        return vertex;
    }
}
