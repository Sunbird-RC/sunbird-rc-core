package dev.sunbirdrc.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.pojos.SearchQuery;
import org.apache.tinkerpop.gremlin.structure.Graph;


public interface SearchDao {

    JsonNode search(Graph graphFromStore, SearchQuery searchQuery, boolean expandInternal);

}
