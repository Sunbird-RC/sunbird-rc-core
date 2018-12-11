package io.opensaber.registry.sink;

import com.steelbridgelabs.oss.neo4j.structure.Neo4JElementIdProvider;
import org.neo4j.driver.v1.types.Entity;

import java.util.Objects;
import java.util.UUID;

public class Neo4jIdProvider implements Neo4JElementIdProvider<String> {
    public String fieldName() {
        return null;
    }

    public String get(Entity entity) {
        Objects.requireNonNull(entity, "entity cannot be null");
        return String.valueOf(entity.id());
    }

    public String generate() {
        return UUID.randomUUID().toString();
    }

    public String processIdentifier(Object id) {
        Objects.requireNonNull(id, "Element identifier cannot be null");
        if (id instanceof String) {
            return (String)id;
        } else {
            throw new IllegalArgumentException(String.format("Expected an id that is convertible to Long but received %s", id.getClass()));
        }
    }

    public String matchPredicateOperand(String alias) {
        Objects.requireNonNull(alias, "alias cannot be null");
        return alias + ".osid";
    }
}


