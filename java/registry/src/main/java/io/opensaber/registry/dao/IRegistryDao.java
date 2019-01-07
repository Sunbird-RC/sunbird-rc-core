package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.registry.util.ReadConfigurator;
import java.util.List;
import java.util.Set;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public interface IRegistryDao {

	void setPrivatePropertyList(List<String> privatePropertyList);
	Vertex ensureParentVertex(Graph graph, String parentLabel);
	List<String> getUUIDs(Graph graph, Set<String> labels);
	String addEntity(JsonNode rootNode);
	JsonNode getEntity(String uuid, ReadConfigurator readConfigurator) throws Exception;
	void updateVertex(Vertex rootVertex, JsonNode inputJsonNode);

}
