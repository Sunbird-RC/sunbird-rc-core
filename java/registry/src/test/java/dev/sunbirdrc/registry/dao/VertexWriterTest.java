package dev.sunbirdrc.registry.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.DBConnectionInfo;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.sink.DBProviderFactory;
import dev.sunbirdrc.registry.sink.DatabaseProvider;
import dev.sunbirdrc.registry.sink.OSGraph;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Environment.class, DBProviderFactory.class, DBConnectionInfoMgr.class, DBConnectionInfo.class})
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
class VertexWriterTest {
    @Autowired
    private DBProviderFactory dbProviderFactory;

    @Autowired
    private DBConnectionInfoMgr dbConnectionInfoMgr;

    private Graph graph;

    private DatabaseProvider mockDatabaseProvider;

    private static final String testUuidPropertyName = "tid";

    private VertexWriter vertexWriter;
    Vertex vertex;

    @BeforeEach
    void setUp() throws Exception {
        dbConnectionInfoMgr.setUuidPropertyName(testUuidPropertyName);
        mockDatabaseProvider = Mockito.mock(DatabaseProvider.class);
        graph = Mockito.mock(Graph.class);
        OSGraph osGraph = Mockito.mock(OSGraph.class);
        Mockito.when(mockDatabaseProvider.getOSGraph()).thenReturn(osGraph);
        Mockito.when(osGraph.getGraphStore()).thenReturn(graph);
        vertex = Mockito.mock(Vertex.class);
        Mockito.when(graph.addVertex(anyString())).thenReturn(vertex);
        vertexWriter = new VertexWriter(graph, mockDatabaseProvider, testUuidPropertyName);
    }

    @Test
    void ensureParentVertexWhenParentIndexAlreadyExists() {
        String parentLabel = "Test_Group";
        GraphTraversalSource graphTraversalSource = Mockito.mock(GraphTraversalSource.class);
        GraphTraversal graphTraversal = Mockito.mock(GraphTraversal.class);
        Mockito.when(graph.traversal()).thenReturn(graphTraversalSource);
        Mockito.when(graphTraversalSource.clone()).thenReturn(graphTraversalSource);
        Mockito.when(graphTraversalSource.V()).thenReturn(graphTraversal);
        Mockito.when(graphTraversal.hasLabel(P.eq(parentLabel))).thenReturn(graphTraversal);
        Mockito.when(graphTraversal.hasNext()).thenReturn(true);
        Mockito.when(graphTraversal.next()).thenReturn(vertex);
        Vertex actualVertex = vertexWriter.ensureParentVertex(parentLabel);
        assertEquals(vertex, actualVertex);
    }

    @Test
    void ensureParentVertexWhenParentIndexDoesNotExist() {
        String parentLabel = "Test_Group";
        GraphTraversalSource graphTraversalSource = Mockito.mock(GraphTraversalSource.class);
        GraphTraversal graphTraversal = Mockito.mock(GraphTraversal.class);
        Mockito.when(graph.traversal()).thenReturn(graphTraversalSource);
        Mockito.when(graphTraversalSource.clone()).thenReturn(graphTraversalSource);
        Mockito.when(graphTraversalSource.V()).thenReturn(graphTraversal);
        Mockito.when(graphTraversal.hasLabel(P.eq(parentLabel))).thenReturn(graphTraversal);
        Mockito.when(graphTraversal.hasNext()).thenReturn(false);
        Mockito.when(vertex.id()).thenReturn("123");
        Vertex actualVertex = vertexWriter.ensureParentVertex(parentLabel);
        assertEquals(vertex, actualVertex);
        Mockito.verify(actualVertex, Mockito.times(1)).property(Constants.INDEX_FIELDS, "");
        Mockito.verify(actualVertex, Mockito.times(1)).property(Constants.UNIQUE_INDEX_FIELDS, "");
    }

    private Vertex createVertexImpl(String lblStr) {
        return vertexWriter.createVertex(lblStr);
    }

    @Test
    void createVertex() {
        String lblStr = "LabelStr1";
        Mockito.when(mockDatabaseProvider.generateId(vertex)).thenReturn("123");
        Vertex vertexCreated = createVertexImpl(lblStr);
        Mockito.verify(vertexCreated, Mockito.times(1)).property("@type", lblStr);
        Mockito.verify(vertexCreated, Mockito.times(1)).property(testUuidPropertyName, "123");
    }

    @Test
    void writeSingleNode() {
        Vertex parentVertex = Mockito.mock(Vertex.class);
        String label = "dummy_lbl";
        ObjectNode entryValue = JsonNodeFactory.instance.objectNode();
        TextNode value = JsonNodeFactory.instance.textNode("value1");
        entryValue.set("field1", value);
        TextNode references = JsonNodeFactory.instance.textNode("did:some_entity:123");
        entryValue.set("references", references);
        Mockito.when(mockDatabaseProvider.getId(vertex)).thenReturn("123");
        Vertex actualVertex = vertexWriter.writeSingleNode(parentVertex, label, entryValue);
        Mockito.verify(vertex, Mockito.times(1)).property("references", "did:some_entity:123");
        Mockito.verify(vertex, Mockito.times(1)).property("field1", "value1");
        Mockito.verify(parentVertex, Mockito.times(1)).addEdge(label, vertex);
        Mockito.verify(parentVertex, Mockito.times(1)).property(label + "_" + testUuidPropertyName, "123");
        assertEquals(vertex, actualVertex);
    }

    @Test
    void addEdge() {
        String eLabel = "testEdgeLabel";
        Vertex v1 = createVertexImpl("v1");
        Vertex v2 = createVertexImpl("v2");

        vertexWriter.addEdge(eLabel, v1, v2);

        Mockito.verify(v1, Mockito.times(1)).addEdge(eLabel, v2);
    }

    @Test
    void test_shouldUpdateParentIndexProperty() {
        List<String> indexFields = new ArrayList<>();
        indexFields.add("name");
        indexFields.add("rollNo");
        Vertex parentVertex = Mockito.mock(Vertex.class);
        String propertyName = "test";
        vertexWriter.updateParentIndexProperty(parentVertex, propertyName, indexFields);
        Mockito.verify(parentVertex, Mockito.times(1)).property(propertyName, "name,rollNo");
    }

    @Test
    void writeNodeEntity() {
        String recordStr = "{\"entityName\": {\"ref\": \"did:anotherEntity:1234\", \"a\":\"b\", \"cObj\": {\"d\":\"e\"}, \"fArr\": [\"i1\", \"i2\"], \"gObjArr\": [{\"i1\": \"v1\"}, {\"i2\":\"v2\"}]}}";
        JsonNode recordNode = null;
        try {
            recordNode = new ObjectMapper().readTree(recordStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        GraphTraversalSource graphTraversalSource = Mockito.mock(GraphTraversalSource.class);
        GraphTraversal graphTraversal = Mockito.mock(GraphTraversal.class);
        VertexProperty vertexProperty = Mockito.mock(VertexProperty.class);
        Mockito.when(mockDatabaseProvider.generateId(any())).thenReturn("123");
        Mockito.when(mockDatabaseProvider.getId(vertex)).thenReturn("123");
        Mockito.when(graph.traversal()).thenReturn(graphTraversalSource);
        Mockito.when(graphTraversalSource.clone()).thenReturn(graphTraversalSource);
        Mockito.when(graphTraversalSource.V()).thenReturn(graphTraversal);
        Mockito.when(graphTraversal.hasLabel("anotherEntity")).thenReturn(graphTraversal);
        Mockito.when(graphTraversal.has(testUuidPropertyName, "1234")).thenReturn(graphTraversal);
        Mockito.when(graphTraversal.hasNext()).thenReturn(true).thenReturn(false);
        Mockito.when(graphTraversal.next()).thenReturn(vertex);
        Mockito.when(vertexProperty.isPresent()).thenReturn(false);
        Mockito.when(vertex.property(anyString())).thenReturn(vertexProperty);
        String id = vertexWriter.writeNodeEntity(recordNode);
        assertEquals("123", id);
    }
}