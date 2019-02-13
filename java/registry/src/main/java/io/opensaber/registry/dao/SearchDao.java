package io.opensaber.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.pojos.SearchQuery;
import org.apache.tinkerpop.gremlin.structure.Graph;


public interface SearchDao {

    JsonNode search(Graph graphFromStore, SearchQuery searchQuery);

}
