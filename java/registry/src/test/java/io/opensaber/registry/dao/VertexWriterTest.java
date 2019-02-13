package io.opensaber.registry.dao;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.sink.DBProviderFactory;
import io.opensaber.registry.sink.DatabaseProvider;
import io.opensaber.registry.sink.OSGraph;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Environment.class, DBProviderFactory.class, DBConnectionInfoMgr.class, DBConnectionInfo.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class VertexWriterTest {
    @Autowired
    private DBProviderFactory dbProviderFactory;

    @Autowired
    private DBConnectionInfoMgr dbConnectionInfoMgr;

    private Graph graph;

    private DatabaseProvider mockDatabaseProvider;

    private static final String testUuidPropertyName = "tid";

    private VertexWriter vertexWriter;

    @Before
    public void setUp() throws Exception {
        dbConnectionInfoMgr.setUuidPropertyName(testUuidPropertyName);

        mockDatabaseProvider = dbProviderFactory.getInstance(null);
        try (OSGraph osGraph = mockDatabaseProvider.getOSGraph()) {
            graph = osGraph.getGraphStore();
            vertexWriter = new VertexWriter(graph, mockDatabaseProvider, testUuidPropertyName);
        }
    }

    @Test
    public void ensureParentVertex() {

    }

    private Vertex createVertexImpl(String lblStr) {
        return vertexWriter.createVertex(lblStr);
    }

    @Test
    public void createVertex() {
        String lblStr = "LabelStr1";
        Vertex vertexCreated = createVertexImpl(lblStr);
        Assert.assertTrue(vertexCreated != null &&
                vertexCreated.label().equals(lblStr) &&
                vertexCreated.value(testUuidPropertyName) != null &&
                vertexCreated.value(Constants.TYPE_STR_JSON_LD) != null);
    }

    @Test
    public void writeSingleNode() {
    }

    @Test
    public void addEdge() {
        String eLabel = "testEdgeLabel";
        Vertex v1 = createVertexImpl("v1");
        Vertex v2 = createVertexImpl("v2");

        vertexWriter.addEdge(eLabel, v1, v2);

        Assert.assertTrue(v1.vertices(Direction.OUT).next() != null &&
                            !v2.vertices(Direction.OUT).hasNext());
    }

    @Test
    public void writeNodeEntity() {
        String recordStr = "{\"entityName\": {\"a\":\"b\", \"cObj\": {\"d\":\"e\"}, \"fArr\": [\"i1\", \"i2\"], \"gObjArr\": [{\"i1\": \"v1\"}, {\"i2\":\"v2\"}]}}";
        JsonNode recordNode = null;
        try {
            recordNode = new ObjectMapper().readTree(recordStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String id = vertexWriter.writeNodeEntity(recordNode);
        Assert.assertTrue(id != null);
    }
}