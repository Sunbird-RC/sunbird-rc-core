package io.opensaber.registry.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.pojos.Filter;
import io.opensaber.pojos.FilterOperators;
import io.opensaber.pojos.SearchQuery;
import io.opensaber.registry.dao.ValueType;
import io.opensaber.registry.sink.shard.Shard;
import io.opensaber.registry.util.ReadConfigurator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IReadService {

    JsonNode getEntity(Shard shard, String userId, String id, String entityType, ReadConfigurator configurator) throws Exception;

}
