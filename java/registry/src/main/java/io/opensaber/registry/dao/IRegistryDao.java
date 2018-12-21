package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.util.ReadConfigurator;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.List;
import java.util.Set;

public interface IRegistryDao {

	void setPrivatePropertyList(List<String> privatePropertyList);
	Vertex ensureParentVertex(Graph graph, String parentLabel);
	String getParentName(JsonNode node);
	String writeNodeEntity(Graph graph, JsonNode node);
	List<String> getUUIDs(Graph graph, Set<String> labels);
	String addEntity(String shardId, JsonNode rootNode);
	JsonNode getEntity(String shardId, String uuid, ReadConfigurator readConfigurator);
	void updateVertex(Vertex rootVertex, JsonNode inputJsonNode);

}
