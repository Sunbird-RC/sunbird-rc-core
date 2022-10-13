package dev.sunbirdrc.registry.service;

import org.apache.tinkerpop.gremlin.structure.Vertex;

public interface RevocationService {
	void storeCredential(String entity, String entityId, String userId, Vertex deletedVertex);
}
