package io.opensaber.registry.util;

import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component("entityParenter")
public class EntityParenter {

    private DefinitionsManager definitionsManager;
    private DBConnectionInfoMgr dbConnectionInfoMgr;

    private Set<String> defintionNames;
    private List<DBConnectionInfo> dbConnectionInfoList;

    /**
     * Holds information about a definition and a list of ShardParents
     */
    private HashMap<String, ShardParentInfoList> definitionShardParentMap;

    @Autowired
    public EntityParenter(DefinitionsManager definitionsManager, DBConnectionInfoMgr dbConnectionInfoMgr) {
        this.definitionsManager = definitionsManager;
        this.dbConnectionInfoMgr = dbConnectionInfoMgr;

        defintionNames = definitionsManager.getAllKnownDefinitions();
        dbConnectionInfoList = dbConnectionInfoMgr.getConnectionInfo();
    }

    /**
     * Creates the parent vertex in all the shards for all default definitions
     * @return
     */
    public Optional<String> createKnownParenters(Graph graph) {
        Optional<String> result;
        String msg = null;

        dbConnectionInfoList.forEach(dbConnectionInfo -> {
            // Get the shard

            defintionNames.forEach(defintionName -> {
                String parentLabel = ParentLabelGenerator.getLabel(defintionName);
                TPGraphMain.createParentVertex(graph, parentLabel);
            });
        });

        result = Optional.of(msg);
        return result;
    }

    /**
     * Reads the database and learns the mappings. This is expected to be called
     * just once.
     * @return
     */
    public void identifyDefinitionParents() {
        defintionNames.forEach(defintionName -> {
        // Create the vertex with label if not already found
            ShardParentInfoList parentInfoList = new ShardParentInfoList();

            dbConnectionInfoList.forEach(dbConnectionInfo -> {
                // TODO
                // Get the shard and fetch uuid from label

                String shardId = "";
                String uuid = "";
                parentInfoList.getParentInfos().add(new ShardParentInfo(shardId, uuid));
            });

            definitionShardParentMap.put(defintionName, parentInfoList);
        });
    }

}
