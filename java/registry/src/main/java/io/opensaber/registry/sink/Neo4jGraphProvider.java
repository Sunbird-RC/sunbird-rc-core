package io.opensaber.registry.sink;

import com.steelbridgelabs.oss.neo4j.structure.Neo4JEdge;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JElementIdProvider;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JVertex;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfo;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;

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

        // TODO: Check with auth
        driver = GraphDatabase.driver(connection.getUri(), AuthTokens.none());
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

    /**
     * For neo4j, we would like to use the Neo4JIdProvider
     * 
     * @param o
     *            - any record object
     * @return
     */
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
    public void createIndex(Graph graph,String label, List<String> propertyNames) {
        Neo4JGraph neo4jGraph = (Neo4JGraph) graph;  
		if (propertyNames.size() > 0) {
			for (String propertyName : propertyNames) {
				neo4jGraph.createIndex(label, propertyName);
				logger.info("Neo4jGraph index created for " + label);
			}
		} else {
			logger.info("Could not create single index for empty properties");
		}
      
    }
    
    @Override
    public void createCompositeIndex(Graph graph, String label, List<String> propertyNames){
        Neo4JGraph neo4jGraph = (Neo4JGraph) graph;
		if (propertyNames.size() > 0) {
			StringBuilder properties = new StringBuilder(String.join(",", propertyNames));
			logger.info("composite key properties values " + properties);

			Objects.requireNonNull(label, "label cannot be null");
			Objects.requireNonNull(properties, "properties cannot be null");
			neo4jGraph.execute(new Statement("CREATE INDEX ON :`" + label + "`(" + properties + ")"));
		} else {
			logger.info("Could not create composite index for empty properties");
		}
    }
    
    @Override
    public void createUniqueIndex(Graph graph, String label, List<String> propertyNames) {
        Neo4JGraph neo4jGraph = (Neo4JGraph) graph;
		if (propertyNames.size() > 0) {

			for (String propertyName : propertyNames) {
				Objects.requireNonNull(label, "label cannot be null");
				Objects.requireNonNull(propertyName, "propertyName cannot be null");
				neo4jGraph.execute(new Statement(
						"CREATE CONSTRAINT ON (n:" + label + ") ASSERT n." + propertyName + " IS UNIQUE"));
				logger.info("Neo4jGraph unique index created for " + label);

			}
		} else {
			logger.info("Could not create unique index for empty properties");
		}
    }
}