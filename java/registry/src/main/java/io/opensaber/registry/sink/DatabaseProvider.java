package io.opensaber.registry.sink;

import io.opensaber.registry.middleware.util.Constants;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class DatabaseProvider {
    private Constants.GraphDatabaseProvider provider;
    private String uuidPropertyName;
    private Optional<Boolean> supportsTransaction = Optional.empty();

    private static Logger logger = LoggerFactory.getLogger(DatabaseProvider.class);

    public abstract void shutdown() throws Exception;

    public abstract OSGraph getOSGraph();

    /**
     * This method is used for checking database service. It fires a dummy query
     * to check for a non-existent label and checks for the count of the
     * vertices
     *
     * @return
     */
    public boolean isDatabaseServiceUp() {
        boolean databaseStatusUp = false;
        try {
            try (OSGraph osGraph = getOSGraph()) {
                Graph graph = osGraph.getGraphStore();

                long count = IteratorUtils.count(graph.traversal().clone().V().has(T.label, "HealthCheckLabel"));
                databaseStatusUp = count >= 0;
            }
        } catch (Exception ex) {
            logger.error("Database service is not running. " + ex);
        }
        return databaseStatusUp;
    }

    /**
     * This method is used to initialize some global graph level configuration
     */
    public void initializeGlobalGraphConfiguration() {
        try {
            try (OSGraph osGraph = getOSGraph()) {
                Graph graph = osGraph.getGraphStore();
                if (IteratorUtils.count(graph.traversal().V().has(T.label, Constants.GRAPH_GLOBAL_CONFIG)) == 0) {
                    logger.info("Adding GRAPH_GLOBAL_CONFIG node...");
                    Transaction tx = startTransaction(graph);
                    Vertex globalConfig = graph.traversal().clone().addV(Constants.GRAPH_GLOBAL_CONFIG).next();
                    globalConfig.property(Constants.PERSISTENT_GRAPH, true);
                    commitTransaction(graph, tx);
                }
            }
        } catch (Exception closeException) {
            logger.debug("Can't initialize global graph configuration ", closeException);
        }
    }

    private boolean supportsTransaction(Graph graph) {
        if(!supportsTransaction.isPresent()){
            supportsTransaction = Optional.ofNullable(graph.features().graph().supportsTransactions());
        }
        return supportsTransaction.get();
    }

    public Transaction startTransaction(Graph graph) {
        Transaction tx = null;
        if (supportsTransaction(graph)) {
            tx = graph.tx();
        }
        return tx;
    }

    /**
     * option to close a graph while commiting
     */
    protected void commitTransaction(Graph graph, Transaction tx, boolean closeGraph) {
        if (null != tx && supportsTransaction(graph)) {
            tx.commit();
        }
        if (closeGraph) {
            try {
                graph.close();
            } catch (Exception e) {
                logger.error("Can't close graph " + e.getMessage());
            }
        }

    }

    /**
     * Default commit transaction used by any caller.
     */
    public void commitTransaction(Graph graph, Transaction tx) {
        if (null != tx && supportsTransaction(graph)) {
            tx.commit();
        }
    }

    /**
     * This menthod will soon be removed.
     */
    public Graph getGraphStore() {
        return getOSGraph().getGraphStore();
    }

    /**
     * For any object agnostic of database class, returns id.
     * CAUTION: Use this only for new nodes
     * @param o - any record object
     * @return
     */
    public String generateId(Object o) {
        return UUID.randomUUID().toString();
    }

    public String getId(Vertex vertex) {
        return (String) vertex.property(getUuidPropertyName()).value();
    }

    public String getId(Edge e) {
        return (String) e.property(getUuidPropertyName()).value();
    }

    public String getUuidPropertyName() {
        return uuidPropertyName;
    }

    protected void setUuidPropertyName(String uuidPropertyName) {
        this.uuidPropertyName = uuidPropertyName;
    }
   
    /**
     * Creates index
     */
    public void createIndex(Graph graph, String label, List<String> propertyNames){
        //Does nothing, suppose to be overridden by extended classes.
    }
    /**
     * Creates unique index
     */
    public void createUniqueIndex(Graph graph, String label, List<String> propertyNames){
        //Does nothing, suppose to be overridden by extended classes.
    }
    /**
     * Creates composite index
     */
    public void createCompositeIndex(Graph graph, String label, List<String> propertyNames){
        //Does nothing, suppose to be overridden by extended classes.
    }
        
    public Constants.GraphDatabaseProvider getProvider() {
        return this.provider;
    }

    protected void setProvider(Constants.GraphDatabaseProvider provider) {
        this.provider = provider;
    }
}
