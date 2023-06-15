package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.pojos.HealthCheckResponse;
import dev.sunbirdrc.registry.model.EventType;
import dev.sunbirdrc.registry.sink.shard.Shard;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public interface RegistryService {

	HealthCheckResponse health(Shard shard) throws Exception;

	Vertex deleteEntityById(Shard shard, String userId, String id, boolean markSignedDataNull) throws Exception;

	String addEntity(Shard shard, String userId, JsonNode inputJson, boolean skipSignature) throws Exception;

	void updateEntity(Shard shard, String userId, String id, String jsonString) throws Exception;

	void callESActors(JsonNode rootNode, String operation, String parentEntityType, String entityRootId, Transaction tx) throws Exception;

	void callNotificationActors(String operation, String to, String subject, String message) throws Exception;
	void maskAndEmitEvent(JsonNode deletedNode, String index, EventType delete, String userId, String uuid) throws JsonProcessingException;

}
