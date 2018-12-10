package io.opensaber.registry.sink;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.umlg.sqlg.structure.SqlgGraph;

public class SqlgProvider extends DatabaseProvider {

	private Logger logger = LoggerFactory.getLogger(SqlgProvider.class);
	private Graph graph;

	public SqlgProvider(Environment environment) {
		String jdbcUrl = environment.getProperty("database.jdbc.url");
		String jdbcUsername = environment.getProperty("database.jdbc.username");
		String jdbcPassword = environment.getProperty("database.jdbc.password");
		Configuration config = new BaseConfiguration();
		config.setProperty("jdbc.url", jdbcUrl);
		config.setProperty("jdbc.username", jdbcUsername);
		config.setProperty("jdbc.password", jdbcPassword);
		graph = SqlgGraph.open(config);
	}

	@Override
	public Graph getGraphStore() {
		return graph;
	}

	@Override
	public Neo4JGraph getNeo4JGraph() {
		return null;
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
}
