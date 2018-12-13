package io.opensaber.registry.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProvider;

@Component("entityParenter")
public class EntityParenter {
	private static Logger logger = LoggerFactory.getLogger(EntityParenter.class);

	@Autowired
	private DBProviderFactory dbProviderFactory;
	private DefinitionsManager definitionsManager;
	private DBConnectionInfoMgr dbConnectionInfoMgr;

	private Set<String> defintionNames;
	private List<DBConnectionInfo> dbConnectionInfoList;
	/**
	 * Holds relation with shard and parentLabels
	 */
	private Map<String, List<String>> shardIdParentLabelsMap;
	/**
	 * Holds information about a definition and a list of ShardParents
	 */
	private HashMap<String, ShardParentInfoList> definitionShardParentMap;

	@Autowired
	public EntityParenter(DefinitionsManager definitionsManager, DBConnectionInfoMgr dbConnectionInfoMgr) {
		this.definitionsManager = definitionsManager;
		this.dbConnectionInfoMgr = dbConnectionInfoMgr;

		defintionNames = this.definitionsManager.getAllKnownDefinitions();
		dbConnectionInfoList = this.dbConnectionInfoMgr.getConnectionInfo();

		definitionShardParentMap = new HashMap<>();
		shardIdParentLabelsMap = new HashMap<>();

	}

	/**
	 * Creates the parent vertex in all the shards for all default definitions
	 * 
	 * @return
	 */
	public Optional<String> ensureKnownParenters() {
		Optional<String> result;

		dbConnectionInfoList.forEach(dbConnectionInfo -> {
			// Get the graph.
			Graph graph = dbProviderFactory.getInstance(dbConnectionInfo).getGraphStore();
	       	if (graph.features().graph().supportsTransactions()) {
	    			org.apache.tinkerpop.gremlin.structure.Transaction tx = graph.tx();
	    			tx.onReadWrite(org.apache.tinkerpop.gremlin.structure.Transaction.READ_WRITE_BEHAVIOR.AUTO);
	    		}
			List<String> parentLabels = new ArrayList<>();
			defintionNames.forEach(defintionName -> {
				String parentLabel = ParentLabelGenerator.getLabel(defintionName);
				parentLabels.add(parentLabel);
				TPGraphMain.createParentVertex(graph, parentLabel);
			});
			shardIdParentLabelsMap.put(dbConnectionInfo.getShardId(), parentLabels);
            graph.tx().commit();
		});

		result = Optional.empty();
		return result;
	}

	/**
	 * Reads the database and learns the mappings. This is expected to be called
	 * just once.
	 * 
	 * @return
	 */
	public void identifyKnownParents() {
		defintionNames.forEach(defintionName -> {
			// Create the vertex with label if not already found
			ShardParentInfoList parentInfoList = new ShardParentInfoList();

			dbConnectionInfoList.forEach(dbConnectionInfo -> {

				String shardId = dbConnectionInfo.getShardId();
				List<String> parentLabels = shardIdParentLabelsMap.get(shardId);
				logger.info("Shard ID :" + shardId + " --  parentlabels: " + parentLabels.size());
				DatabaseProvider dbProvider = dbProviderFactory.getInstance(dbConnectionInfo);
				List<String> uuids = TPGraphMain.getUUIDs(parentLabels, dbProvider);
				for (String uuid : uuids) {
					parentInfoList.getParentInfos().add(new ShardParentInfo(shardId, uuid));
				}
			});
			logger.info("definitionShardParentMap adding key: " + defintionName + " parentInfoList: "
					+ parentInfoList.getParentInfos().size());
			definitionShardParentMap.put(defintionName, parentInfoList);
		});
	}

}
