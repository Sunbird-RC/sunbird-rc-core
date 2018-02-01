package io.opensaber.registry.util;

import java.io.File;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.tinkerpop.api.impl.Neo4jGraphAPIImpl;

public class GraphDBFactory {

	private static Neo4jGraph graph;

	private static GraphDatabaseService graphDBService;

	public static void initializeGraphDb() {
		if(graph==null || graphDBService == null || !graphDBService.isAvailable(0))	{
			Configuration config = new BaseConfiguration();
			config.setProperty(Neo4jGraph.CONFIG_DIRECTORY, "/var/lib/neo4j/data/databases/graph.db");
			config.setProperty("gremlin.neo4j.conf.cache_type", "none");
			graph = Neo4jGraph.open(config);

		}
	}

	public static Neo4jGraph getGraphDB(){
		initializeGraphDb();
		return graph;
	}


	public static GraphDatabaseService getGraphDatabaseService() {
		if(graphDBService ==null || !graphDBService.isAvailable(0)){
			graphDBService = ((Neo4jGraphAPIImpl) getGraphDB().getBaseGraph()).getGraphDatabase();
		}
		return graphDBService;
	}
}
