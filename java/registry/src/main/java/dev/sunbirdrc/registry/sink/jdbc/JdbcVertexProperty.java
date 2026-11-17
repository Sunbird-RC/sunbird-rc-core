package dev.sunbirdrc.registry.sink.jdbc;

import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;

import java.util.*;

/**
 * VertexProperty implementation for JDBC-backed vertices.
 * Works with per-entity tables (V_{label} naming convention).
 */
public class JdbcVertexProperty<V> implements VertexProperty<V> {

    private final JdbcVertex vertex;
    private final String key;
    private final V value;
    private final String id;

    public JdbcVertexProperty(JdbcVertex vertex, String key, V value) {
        this.vertex = vertex;
        this.key = key;
        this.value = value;
        this.id = vertex.id() + ":" + key;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public V value() throws NoSuchElementException {
        if (value == null) {
            throw new NoSuchElementException("Property value is null");
        }
        return value;
    }

    @Override
    public boolean isPresent() {
        return value != null;
    }

    @Override
    public Vertex element() {
        return vertex;
    }

    @Override
    public void remove() {
        vertex.getPropertiesMap().remove(key);
        // Pass vertex label for per-entity table update
        ((JdbcGraph) vertex.graph()).removeVertexProperty((Long) vertex.id(), vertex.label(), key);
    }

    @Override
    public Object id() {
        return id;
    }

    @Override
    public <U> Property<U> property(String key, U value) {
        throw new UnsupportedOperationException("Meta-properties are not supported");
    }

    @Override
    public <U> Iterator<Property<U>> properties(String... propertyKeys) {
        // Meta-properties not supported
        return Collections.emptyIterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VertexProperty)) return false;
        VertexProperty<?> that = (VertexProperty<?>) o;
        return Objects.equals(id, that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "vp[" + key + "->" + value + "]";
    }
}
