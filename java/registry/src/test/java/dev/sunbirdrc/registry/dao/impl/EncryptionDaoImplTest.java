package dev.sunbirdrc.registry.dao.impl;

import com.google.gson.Gson;
import dev.sunbirdrc.registry.config.GenericConfiguration;
import dev.sunbirdrc.registry.controller.RegistryTestBase;
import dev.sunbirdrc.registry.dao.IRegistryDao;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.AuditRecordReader;
import dev.sunbirdrc.registry.service.impl.EncryptionServiceImpl;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {Environment.class, ObjectMapper.class, GenericConfiguration.class,
        EncryptionServiceImpl.class, AuditRecordReader.class})
@TestMethodOrder(MethodOrderer.MethodName.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
class EncryptionDaoImplTest extends RegistryTestBase {
    private static Logger logger = LoggerFactory.getLogger(EncryptionDaoImplTest.class);
    private static Graph graph;

    @Autowired
    AuditRecordReader auditRecordReader;

    @Autowired
    private IRegistryDao registryDao;
    @Autowired
    private Gson gson;
    @Autowired
    private EncryptionServiceImpl encryptionMock;

    @Value("${encryption.enabled}")
    private boolean encryptionEnabled;

    @BeforeEach
    void initializeGraph() throws IOException {
        auditRecordReader = new AuditRecordReader(databaseProvider);
        Assumptions.assumeTrue(encryptionEnabled);
    }

    @AfterEach
    void shutDown() throws Exception {
        if (graph != null) {
            graph.close();
        }
    }

    Vertex getVertexForSubject(String subjectValue, String property, String objectValue) {
        Vertex vertex = null;
        graph = TinkerGraph.open();
        GraphTraversalSource t = graph.traversal();
        GraphTraversal<Vertex, Vertex> hasLabel = t.V().hasLabel(subjectValue);
        if (hasLabel.hasNext()) {
            vertex = hasLabel.next();
        } else {
            vertex = graph.addVertex(T.label, subjectValue);
        }
        vertex.property(property, objectValue);
        return vertex;
    }

    Vertex getVertexWithMultipleProperties(String subjectValue, Map<String, Object> map) {
        Vertex vertex = null;
        graph = TinkerGraph.open();
        GraphTraversalSource t = graph.traversal();
        GraphTraversal<Vertex, Vertex> hasLabel = t.V().hasLabel(subjectValue);
        if (hasLabel.hasNext()) {
            vertex = hasLabel.next();
        } else {
            vertex = graph.addVertex(T.label, subjectValue);
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            vertex.property(entry.getKey(), entry.getValue());
        }
        return vertex;
    }
}