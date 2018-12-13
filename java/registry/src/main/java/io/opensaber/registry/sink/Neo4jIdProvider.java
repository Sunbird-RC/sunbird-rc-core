package io.opensaber.registry.sink;

import com.steelbridgelabs.oss.neo4j.structure.Neo4JElementIdProvider;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.v1.types.Entity;

import java.util.Objects;
import java.util.UUID;

public class Neo4jIdProvider implements Neo4JElementIdProvider<String> {
    private String uuidPropertyName;

    public String fieldName() {
        return uuidPropertyName;
    }

    /**
     * When read back using labels, the library gives back only the id.
     * Here we are loading only the extra osid for our convenience.
     * @param entity
     * @return
     */
    public String get(Entity entity) {
        Objects.requireNonNull(entity, "entity cannot be null");
        String neo4jId = String.valueOf(entity.id());
        String givenId = neo4jId;
        if (entity instanceof InternalNode) {
            givenId = entity.get(uuidPropertyName).asString();
        }
        return givenId;
    }

    /**
     * This is the globally unique identifier we want to use for every
     * record.
     * @return
     */
    public String generate() {
        return UUID.randomUUID().toString();
    }

    public String processIdentifier(Object id) {
        Objects.requireNonNull(id, "Element identifier cannot be null");
        if (id instanceof String) {
            return (String) id;
        } else {
            throw new IllegalArgumentException(String.format("Expected an id that is convertible to Long but received %s", id.getClass()));
        }
    }

    public String matchPredicateOperand(String alias) {
        Objects.requireNonNull(alias, "alias cannot be null");
        return alias + "." + uuidPropertyName;
    }

    public String getUuidPropertyName() {
        return uuidPropertyName;
    }

    public void setUuidPropertyName(String uuidPropertyName) {
        this.uuidPropertyName = uuidPropertyName;
    }

}


