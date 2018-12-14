package io.opensaber.registry.util;

import org.apache.tinkerpop.gremlin.structure.Vertex;

public class ShardParentInfo {
    /**
     * The definition
     */
    private String name;
    private String uuid;
    private Vertex vertex;

    public ShardParentInfo(String name, Vertex vertex) {
        this.name = name;
        this.vertex = vertex;
    }

    public String getName() {
        return name;
    }

    public void setName(String shardId) {
        this.name = shardId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public void setVertex(Vertex vertex) {
        this.vertex = vertex;
    }
}
