package io.opensaber.registry.sink;

import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfo;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.umlg.sqlg.structure.SqlgGraph;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class SqlgProvider extends DatabaseProvider {

	private Logger logger = LoggerFactory.getLogger(SqlgProvider.class);
	private SqlgGraph graph;
	private OSGraph customGraph;

	public SqlgProvider(DBConnectionInfo connectionInfo, String uuidPropertyName) {
		Configuration config = new BaseConfiguration();
		config.setProperty("jdbc.url", connectionInfo.getUri());
		config.setProperty("jdbc.username", connectionInfo.getUsername());
		config.setProperty("jdbc.password", connectionInfo.getPassword());
		setProvider(Constants.GraphDatabaseProvider.SQLG);
		setUuidPropertyName(uuidPropertyName);
		graph = SqlgGraph.open(config);
		customGraph = new OSGraph(graph, false);
	}

	@PostConstruct
	public void init() {
		logger.info("**************************************************************************");
		logger.info("Initializing SQLG DB instance ...");
		logger.info("**************************************************************************");
	}

	@PreDestroy
	public void shutdown() throws Exception {
		logger.info("**************************************************************************");
		logger.info("Gracefully shutting down SQLG DB instance ...");
		logger.info("**************************************************************************");
		graph.close();
	}

	@Override
	public OSGraph getOSGraph() {
		return customGraph;
	}

	@Override
	public String getId(Vertex vertex) {
		return (String) vertex.property(getUuidPropertyName()).value();
	}
}
