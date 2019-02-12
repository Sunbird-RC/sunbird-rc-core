package io.opensaber.registry.util;

import io.opensaber.registry.dao.VertexWriter;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("entityParenter")
public class EntityParenter {
    private static Logger logger = LoggerFactory.getLogger(EntityParenter.class);

    @Autowired
    private IndexHelper indexHelper;
    
    @Value("${database.uuidPropertyName}")
    public String uuidPropertyName;
    
    @Autowired
    private DBProviderFactory dbProviderFactory;

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
     * Loads all the definitions for each shard 
     */
    public void loadDefinitionIndex() {
        Map<String, Boolean> indexMap = new ConcurrentHashMap<String, Boolean>();

        for (Map.Entry<String, ShardParentInfoList> entry : shardParentMap.entrySet()){
          String shardId = entry.getKey();
          ShardParentInfoList shardParentInfoList = entry.getValue();
          shardParentInfoList.getParentInfos().forEach(shardParentInfo->{
              Definition definition = definitionsManager.getDefinition(shardParentInfo.getName());
              Vertex parentVertex = shardParentInfo.getVertex();
              List<String> indexFields = definition.getOsSchemaConfiguration().getIndexFields();
              List<String> indexUniqueFields = definition.getOsSchemaConfiguration().getUniqueIndexFields();
              
              List<String> compositeIndexFields = IndexHelper.getCompositeIndexFields(indexFields);
              List<String> singleIndexFields = IndexHelper.getSingleIndexFields(indexFields);

              int nNewIndices = indexHelper.getNewFields(parentVertex, singleIndexFields, false).size();
              int nNewUniqIndices = indexHelper.getNewFields(parentVertex, indexUniqueFields, true).size();
              int nNewCompIndices = indexHelper.getNewFields(parentVertex, compositeIndexFields, false).size();
            
              boolean indexingComplete = (nNewIndices == 0 && nNewUniqIndices == 0 && nNewCompIndices == 0);
              indexHelper.updateDefinitionIndex(shardId, definition.getTitle(), indexingComplete);
              logger.info("On loadDefinitionIndex for Shard:"+ shardId + " definition: {} updated index to {} ", definition.getTitle(), indexingComplete);
              
          });
          
        }        
        indexHelper.setDefinitionIndexMap(indexMap);
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

                            VertexWriter vertexWriter = new VertexWriter(graph, dbProvider, uuidPropertyName);
                            Vertex v = vertexWriter.ensureParentVertex(parentLabel);

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
    /**
     * Indices gets added 
     */
    public void ensureIndexExists(){        
        dbConnectionInfoList.forEach(dbConnectionInfo -> {
            DatabaseProvider dbProvider = dbProviderFactory.getInstance(dbConnectionInfo);
            try {
                try (OSGraph osGraph = dbProvider.getOSGraph()) {
                    Graph graph = osGraph.getGraphStore();
                    try (Transaction tx = dbProvider.startTransaction(graph)) {
                        List<String> parentLabels = new ArrayList<>();
                        defintionNames.forEach(defintionName -> {
                            
                            String parentLabel = ParentLabelGenerator.getLabel(defintionName);
                            parentLabels.add(parentLabel);
                            VertexWriter vertexWriter = new VertexWriter(graph, dbProvider, uuidPropertyName);
                            Vertex v = vertexWriter.ensureParentVertex(parentLabel);

                            //adding index to shard
                            Definition definition = definitionsManager.getDefinition(defintionName);
                            ensureIndexExists(dbProvider, v, definition, dbConnectionInfo.getShardId());
                        });
                        dbProvider.commitTransaction(graph, tx);
                    }
                }
                
            } catch (Exception e) {
                logger.error("ensureIndex error: {}", e);
            }
        });

        
 
    }
    
    /**
     * Ensures index for a vertex exists Unique index and non-unique index is
     * supported
     * 
     * @param dbProvider
     * @param parentVertex
     * @param definition
     */
    public  void ensureIndexExists(DatabaseProvider dbProvider, Vertex parentVertex, Definition definition, String shardId) {
        try{
            if(!indexHelper.isIndexPresent(definition, shardId)){
                logger.info("Adding index to shard: {} for definition: {}", shardId, definition.getTitle() );
                asyncAddIndex(dbProvider,shardId, parentVertex, definition);
            }
        }catch(Exception e){
            logger.error("ensureIndexExists: Can't create index on table {} for shardId: {} ", definition.getTitle(), shardId);
        }
    }
    
    /**
     * Adds indices to the given label(vertex/table) asynchronously
     * @param dbProvider
     * @param parentVertex
     * @param definition
     */
    private void asyncAddIndex(DatabaseProvider dbProvider, String shardId, Vertex parentVertex, Definition definition) {
        new Thread(() -> {
            if(parentVertex != null && definition != null){
                boolean status = false;
                try {
                    OSGraph osGraph = dbProvider.getOSGraph();
                    Graph graph = osGraph.getGraphStore();
                    Transaction tx = dbProvider.startTransaction(graph);           
                    
                        
                        List<String> indexFields = definition.getOsSchemaConfiguration().getIndexFields();
                        if(!indexFields.contains(uuidPropertyName)){
                            // adds default field (uuid)
                            indexFields.add(uuidPropertyName);
                        }
                        List<String> cIndexFields = IndexHelper.getCompositeIndexFields(indexFields);
                        List<String> sIndexFields = IndexHelper.getSingleIndexFields(indexFields);
                        List<String> newSIndexFields = indexHelper.getNewFields(parentVertex, sIndexFields, false);
                       
                        List<String> indexUniqueFields = definition.getOsSchemaConfiguration().getUniqueIndexFields();               
                        List<String> newUniqueIndexFields = indexHelper.getNewFields(parentVertex, indexUniqueFields, true);
                        
                        Indexer indexer = new Indexer(dbProvider);
                        indexer.setSingleIndexFields(newSIndexFields);
                        indexer.setCompositeIndexFields(cIndexFields);

                        indexer.setUniqueIndexFields(newUniqueIndexFields);
                        status = indexer.createIndex(graph, definition.getTitle(), parentVertex);
                        logger.info("CREATE INDEX STATUS ====== " + status);
                        if(status){
                            indexHelper.updateParentIndexProperty(parentVertex, indexFields, indexUniqueFields);
                        }
                                  
                    dbProvider.commitTransaction(graph, tx);
                    indexHelper.updateDefinitionIndex(shardId, definition.getTitle(), status); 

                    
                } catch (Exception e) {
                    status = false;
                    logger.error(e.getMessage());
                    logger.error("Failed Transaction creating index {}", definition.getTitle());
                }
            } else {
                logger.info("No definition found for create index");
            } 
            
        }).start();           
    }

}
