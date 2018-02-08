package io.opensaber.registry.util;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.neo4j.graphdb.GraphDatabaseService;
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
	
	public static Graph getEmptyGraph(){
		return TinkerGraph.open();
	}
}
