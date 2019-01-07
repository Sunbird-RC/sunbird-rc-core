package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.util.ReadConfigurator;
import java.util.List;
import java.util.Set;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public interface IRegistryDao {

	Vertex ensureParentVertex(Graph graph, String parentLabel);
	List<String> getUUIDs(Graph graph, Set<String> labels);
	String addEntity(Graph graph, JsonNode rootNode);
	JsonNode getEntity(Graph graph, String uuid, ReadConfigurator readConfigurator) throws Exception;
	JsonNode getEntity(Graph graph, Vertex vertex, ReadConfigurator readConfigurator) throws Exception;
	void updateVertex(Graph graph, Vertex rootVertex, JsonNode inputJsonNode);

}
