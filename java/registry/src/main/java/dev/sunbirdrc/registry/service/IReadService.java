package dev.sunbirdrc.registry.service;

import com.fasterxml.jackson.databind.JsonNode;

import dev.sunbirdrc.registry.sink.shard.Shard;
import dev.sunbirdrc.registry.util.ReadConfigurator;

public interface IReadService {

    JsonNode getEntity(Shard shard, String userId, String id, String entityType, ReadConfigurator configurator) throws Exception;

}
