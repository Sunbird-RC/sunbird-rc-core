package io.opensaber.registry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.pojos.HealthCheckResponse;
import io.opensaber.pojos.PluginRequestMessage;
import io.opensaber.registry.sink.shard.Shard;
import org.apache.tinkerpop.gremlin.structure.Transaction;

import javax.servlet.http.HttpServletRequest;

public interface RegistryService {

	HealthCheckResponse health(Shard shard) throws Exception;

	void deleteEntityById(Shard shard, String userId, String id) throws Exception;

	String addEntity(Shard shard, String userId, JsonNode inputJson) throws Exception;

	void updateEntity(Shard shard, String userId, String id, String jsonString) throws Exception;

	void callESActors(JsonNode rootNode, String operation, String parentEntityType, String entityRootId, Transaction tx) throws Exception;

	void callNotificationActors(String operation, String to, String subject, String message) throws Exception;

	void callPluginActors(String actorName, PluginRequestMessage pluginRequestMessage) throws JsonProcessingException;

	void callAutoAttestationActor(JsonNode existingNode, JsonNode updatedNode, String entityName, String entityId, HttpServletRequest request) throws Exception;
}
