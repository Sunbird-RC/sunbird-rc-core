package io.opensaber.registry.sink;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.neo4j.driver.v1.AuthToken;
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
	private Graph graph;

	public Neo4jGraphProvider(DBConnectionInfo connection) {

		try {
			AuthToken authToken = AuthTokens.basic(connection.getUsername(), connection.getPassword());
			if (authToken != null) {
				driver = GraphDatabase.driver(connection.getUri(), authToken);
			} else {
				driver = GraphDatabase.driver(connection.getUri(), AuthTokens.none());
			}

			Neo4JElementIdProvider<?> idProvider = new Neo4JNativeElementIdProvider();
			Neo4JGraph neo4JGraph = new Neo4JGraph(driver, idProvider, idProvider);
			// neo4JGraph.setProfilerEnabled(profilerEnabled);
			graph = neo4JGraph;
			logger.info("Initializing remote graph db for " + connection.getShardId());
		} catch (Exception ex) {
			logger.error("Exception when initializing Neo4J DB connection...", ex);
			throw ex;
		}
	}

	@Override
	public Graph getGraphStore() {
		return graph;
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
		graph.close();
		if (driver != null) {
			driver.close();
		}
	}

}
