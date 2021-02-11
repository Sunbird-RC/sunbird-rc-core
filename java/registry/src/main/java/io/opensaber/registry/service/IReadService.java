package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;

import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.ReadConfigurator;

public interface IReadService {

    JsonNode getEntity(Shard shard, String userId, String id, String entityType, ReadConfigurator configurator) throws Exception;

}
