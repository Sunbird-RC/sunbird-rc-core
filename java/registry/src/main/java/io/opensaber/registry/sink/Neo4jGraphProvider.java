package io.opensaber.registry.sink;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.steelbridgelabs.oss.neo4j.structure.Neo4JElementIdProvider;
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import com.steelbridgelabs.oss.neo4j.structure.providers.Neo4JNativeElementIdProvider;

import io.opensaber.registry.model.DBConnectionInfo;

public class Neo4jGraphProvider extends DatabaseProvider {

	private Logger logger = LoggerFactory.getLogger(Neo4jGraphProvider.class);
	private Driver driver;
	private boolean profilerEnabled;
	private DBConnectionInfo connectionInfo;
	private Neo4JElementIdProvider<?> idProvider = new Neo4JNativeElementIdProvider();

	public Neo4jGraphProvider(DBConnectionInfo connection) {
		connectionInfo = connection;
		profilerEnabled = connection.isProfilerEnabled();
		// TODO: Check with auth
		driver = GraphDatabase.driver(connection.getUri(),
				AuthTokens.none());
		logger.info("Initialized db at ", connectionInfo.getUri());
	}


	private Neo4JGraph getGraph() {
		Neo4JGraph neo4jGraph;
		neo4jGraph = new Neo4JGraph(driver, idProvider, idProvider);
		logger.info("Initialized db at {}", connectionInfo.getUri());
		return neo4jGraph;
	}

	@Override
	public Graph getGraphStore() {
		return getGraph();
	}

	// TODO: We must have an abstract class to allow this possibility.
	@Override
	public Neo4JGraph getNeo4JGraph() {
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