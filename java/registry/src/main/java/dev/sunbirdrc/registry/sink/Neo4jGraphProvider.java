package dev.sunbirdrc.registry.sink;

import com.steelbridgelabs.oss.neo4j.structure.Neo4JEdge;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JElementIdProvider;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JVertex;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.DBConnectionInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Neo4jGraphProvider extends DatabaseProvider {

    private Logger logger = LoggerFactory.getLogger(Neo4jGraphProvider.class);
    private Driver driver;
    private boolean profilerEnabled;
    private DBConnectionInfo connectionInfo;
    private Neo4jIdProvider neo4jIdProvider = new Neo4jIdProvider();

    public Neo4jGraphProvider(DBConnectionInfo connection, String uuidPropName) {
        connectionInfo = connection;
        profilerEnabled = connection.isProfilerEnabled();
        setProvider(Constants.GraphDatabaseProvider.NEO4J);
        setUuidPropertyName(uuidPropName);

        // Use appropriate authentication tokens
        driver = GraphDatabase.driver(connection.getUri(), AuthTokens.basic(connection.getUsername(), connection.getPassword()));
        neo4jIdProvider.setUuidPropertyName(getUuidPropertyName());
        logger.info("Initialized db driver at {}", connectionInfo.getUri());
    }

    private Neo4JGraph getGraph() {
        Neo4JGraph neo4jGraph;
        Neo4JElementIdProvider<?> idProvider = neo4jIdProvider;

        neo4jGraph = new Neo4JGraph(driver, idProvider, idProvider);
        logger.debug("Getting a new graph unit of work");
        return neo4jGraph;
    }

    @PostConstruct
    public void init() {
        logger.info("**************************************************************************");
        logger.info("Initializing Neo4J GraphDB instance ...");
        logger.info("**************************************************************************");
    }

    @PreDestroy
    public void shutdown() throws Exception {
        logger.info("**************************************************************************");
        logger.info("Gracefully shutting down Neo4J GraphDB instance ...");
        logger.info("**************************************************************************");
        driver.close();
    }

    @Override
    public OSGraph getOSGraph() {
        Graph graph = getGraph();
        return new OSGraph(graph, true);
    }

    @Override
    public void commitTransaction(Graph graph, Transaction tx) {
        commitTransaction(graph, tx, true);
    }

    @Override
    public String generateId(Object o) {
        if (o instanceof Neo4JVertex) {
            return ((Neo4JVertex) o).id().toString();
        } else if (o instanceof Neo4JEdge) {
            return ((Neo4JEdge) o).id().toString();
        }

        throw new RuntimeException(o.getClass().getTypeName() + " cannot have an id");
    }

    @Override
    public String getId(Vertex vertex) {
        return vertex.id().toString();
    }

    @Override
    public String getId(Edge edge) {
        return edge.id().toString();
    }

    @Override
    public void createIndex(Graph graph, String label, List<String> propertyNames) {
        if (propertyNames.size() > 0) {
            try (Session session = driver.session()) {
                for (String propertyName : propertyNames) {
                    session.writeTransaction((TransactionWork<Void>) tx -> {
                        tx.run("CREATE INDEX ON :`" + label + "`(" + propertyName + ")");
                        return null;
                    });
                    logger.info("Neo4jGraph index created for " + label);
                }
            } catch (Neo4jException e) {
                logger.error("Failed to create index: ", e);
            }
        } else {
            logger.info("Could not create single index for empty properties");
        }
    }

    @Override
    public void createCompositeIndex(Graph graph, String label, List<String> propertyNames) {
        if (propertyNames.size() > 0) {
            String properties = String.join(",", propertyNames);
            try (Session session = driver.session()) {
                session.writeTransaction((TransactionWork<Void>) tx -> {
                    tx.run("CREATE INDEX ON :`" + label + "`(" + properties + ")");
                    return null;
                });
                logger.info("Neo4jGraph composite index created for " + label);
            } catch (Neo4jException e) {
                logger.error("Failed to create composite index: ", e);
            }
        } else {
            logger.info("Could not create composite index for empty properties");
        }
    }

    @Override
    public void createUniqueIndex(Graph graph, String label, List<String> propertyNames) {
        if (propertyNames.size() > 0) {
            try (Session session = driver.session()) {
                for (String propertyName : propertyNames) {
                    session.writeTransaction((TransactionWork<Void>) tx -> {
                        tx.run("CREATE CONSTRAINT ON (n:" + label + ") ASSERT n." + propertyName + " IS UNIQUE");
                        return null;
                    });
                    logger.info("Neo4jGraph unique index created for " + label);
                }
            } catch (Neo4jException e) {
                logger.error("Failed to create unique index: ", e);
            }
        } else {
            logger.info("Could not create unique index for empty properties");
        }
    }
}