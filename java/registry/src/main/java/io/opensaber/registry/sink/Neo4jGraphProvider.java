package io.opensaber.registry.sink;

import com.steelbridgelabs.oss.neo4j.structure.Neo4JElementIdProvider;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import com.steelbridgelabs.oss.neo4j.structure.providers.Neo4JNativeElementIdProvider;
import io.opensaber.registry.model.DBConnectionInfo;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class Neo4jGraphProvider extends DatabaseProvider {

	private Logger logger = LoggerFactory.getLogger(Neo4jGraphProvider.class);
	private Driver driver;
	private boolean profilerEnabled;
	private DBConnectionInfo connectionInfo;
	private Neo4jIdProvider neo4jIdProvider = new Neo4jIdProvider();

	@Value("${database.uuidPropertyName}")
	public String uuidPropertyName = "osid";

	public Neo4jGraphProvider(DBConnectionInfo connection) {
		connectionInfo = connection;
		profilerEnabled = connection.isProfilerEnabled();
		// TODO: Check with auth
		driver = GraphDatabase.driver(connection.getUri(),
				AuthTokens.none());
		neo4jIdProvider.setUuidPropertyName(uuidPropertyName);
		logger.info("Initialized db driver at {}", connectionInfo.getUri());
	}


	private Neo4JGraph getGraph() {
		Neo4JGraph neo4jGraph;
		Neo4JElementIdProvider<?> idProvider = neo4jIdProvider;

		neo4jGraph = new Neo4JGraph(driver, idProvider, idProvider);
		logger.debug("Getting a new graph unit of work");
		return neo4jGraph;
	}

	@Override
	public Graph getGraphStore() {
		return getGraph();
	}

	// TODO: We must have an abstract class to allow this possibility.
	@Override
	public Neo4JGraph getRawGraph() {
		return getGraph();
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

		if (driver != null) {
			driver.close();
		}
	}
}