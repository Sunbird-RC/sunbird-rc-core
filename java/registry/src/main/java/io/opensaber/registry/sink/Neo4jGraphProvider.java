package io.opensaber.registry.sink;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
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


	private static int nCalls = 0;

	private Graph getGraph() {
		Neo4jGraphProvider.nCalls++;
		logger.info("number of calls " + nCalls);

		Graph graph;
		Boolean isDatabaseEmbedded = false;
		if (isDatabaseEmbedded) {
			// We don't want to run in embedded mode; maybe allowed only for expert users.
			String graphDbLocation = "/data"; // INIT this to neo4j data directory
			logger.info(String.format("Initializing graph db at %s ...", graphDbLocation));
			Configuration config = new BaseConfiguration();
			config.setProperty(Neo4jGraph.CONFIG_DIRECTORY, graphDbLocation);
			config.setProperty("gremlin.neo4j.conf.cache_type", "none");
			graph = Neo4jGraph.open(config);
		} else {
			try {
				Neo4JGraph neo4JGraph = new Neo4JGraph(driver, idProvider, idProvider);
				neo4JGraph.setProfilerEnabled(profilerEnabled);
				graph = neo4JGraph;

			} catch (Exception ex) {
				logger.error("Exception when initializing Neo4J DB connection...", ex);
				throw ex;
			}
		}
		return graph;
	}

	@Override
	public Graph getGraphStore() {
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
