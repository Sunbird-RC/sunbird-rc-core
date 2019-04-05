package io.opensaber.registry.sink;

import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfo;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.VertexLabel;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

public class JanusGraphStorage extends DatabaseProvider {

	private Logger logger = LoggerFactory.getLogger(JanusGraphStorage.class);
	private JanusGraph graph;
	private OSGraph osGraph;

	public JanusGraphStorage(Environment environment, DBConnectionInfo connectionInfo, String uuidPropertyName) {
		Configuration config = new BaseConfiguration();
		String backend = environment.getProperty("cassandra.backend");
		config.setProperty("storage.backend", backend);
		config.setProperty("query.batch", true);

		String host = environment.getProperty("cassandra.hostname");
		config.setProperty("storage.hostname", host);

		String keyspaceName = environment.getProperty("cassandra.keyspace");
		if (keyspaceName != null && !keyspaceName.isEmpty()) {
			config.setProperty("storage.cql.keyspace", keyspaceName);
		}
		config.setProperty("storage.cql.compact-storage", false);
		config.setProperty("storage.cql.compression", false);

		setProvider(Constants.GraphDatabaseProvider.CASSANDRA);
		setUuidPropertyName(uuidPropertyName);
		graph = JanusGraphFactory.open(config);
		osGraph = new OSGraph(graph, false);
	}

	public JanusGraphStorage(Environment environment) {
		String graphFactory = environment.getProperty("database.janus_cassandra.graphFactory");
		String storageBackend = environment.getProperty("database.janus_cassandra.storage.backend");
		String hostname = environment.getProperty("database.janus_cassandra.storage.hostname");
		String storageKeyspace = environment.getProperty("database.janus_cassandra.storage.keyspace");
		String dbCacheSize = environment.getProperty("database.janus_cassandra.db.cache.size");
		String dbCacheCleanUpWaitTime = environment.getProperty("database.janus_cassandra.db.cache.clean.wait");
		String searchIndex = environment.getProperty("database.janus_cassandra.index.storage.backend");
		String searchHostname = environment.getProperty("database.janus_cassandra.index.hostname");
		setProvider(Constants.GraphDatabaseProvider.CASSANDRA);

		Configuration config = new BaseConfiguration();
		config.setProperty("gremlin.graph", graphFactory);
		config.setProperty("storage.backend", storageBackend);
		config.setProperty("storage.cassandra.keyspace", storageKeyspace);
		config.setProperty("storage.hostname", hostname);
		config.setProperty("index.search.backend", searchIndex);
		config.setProperty("index.search.hostname", searchHostname);
		config.setProperty("cache.db-cache-size", Float.parseFloat(dbCacheSize));
		config.setProperty("cache.db-cache-clean-wait", Integer.parseInt(dbCacheCleanUpWaitTime));

		graph = JanusGraphFactory.open(config);
		osGraph = new OSGraph(graph, false);
	}

	@Override
	public void createUniqueIndex(Graph graph, String label, List<String> propertyNames) {
		if (propertyNames.size() > 0) {
			List<JanusGraphIndex> graphIndexList = new ArrayList<>();
			VertexLabel vlabel = ((JanusGraph) graph).getVertexLabel(label);
			graph.tx().commit();
			JanusGraphManagement janusGraphManagement = ((JanusGraph) graph).openManagement();
			propertyNames.forEach(propertyName -> {
				PropertyKey propertyKey = janusGraphManagement.getPropertyKey(propertyName);
				JanusGraphIndex graphIndex = janusGraphManagement.buildIndex(vlabel.name() + propertyKey.toString(), Vertex.class).addKey(propertyKey).unique().buildCompositeIndex();
				graphIndexList.add(graphIndex);
			});
			janusGraphManagement.commit();
		} else {
			logger.info("Could not create unique index for empty properties");
		}

	}

	@Override
	public void createIndex(Graph graph, String label, List<String> propertyNames) {
		if (propertyNames.size() > 0) {
			List<JanusGraphIndex> graphIndexList = new ArrayList<>();
			VertexLabel vlabel = ((JanusGraph) graph).getVertexLabel(label);
			graph.tx().commit();
			JanusGraphManagement janusGraphManagement = ((JanusGraph) graph).openManagement();
			propertyNames.forEach(propertyName -> {
				PropertyKey propertyKey = janusGraphManagement.getPropertyKey(propertyName);
				JanusGraphIndex graphIndex = janusGraphManagement.buildIndex(vlabel.name() + propertyKey.toString(), Vertex.class).addKey(propertyKey).buildCompositeIndex();
				graphIndexList.add(graphIndex);
			});
			janusGraphManagement.commit();
			
		} else {
			logger.info("Could not create single index for empty properties");
		}
	}

	@Override
	public void createCompositeIndex(Graph graph, String label, List<String> propertyNames) {
		if (propertyNames.size() > 0) {
			createIndex(graph, label, propertyNames);
		} else {
			logger.info("Could not create composite index for empty properties");

		}

	}


	@Override
	public OSGraph getOSGraph() {
		return osGraph;
	}

	@PostConstruct
	public void init() {
		logger.info("**************************************************************************");
		logger.info("Initializing Janus GraphDB instance ...");
		logger.info("**************************************************************************");
	}

	@PreDestroy
	public void shutdown() throws Exception {
		logger.info("**************************************************************************");
		logger.info("Gracefully shutting down Janus GraphDB instance ...");
		logger.info("**************************************************************************");
		graph.close();
	}
}
