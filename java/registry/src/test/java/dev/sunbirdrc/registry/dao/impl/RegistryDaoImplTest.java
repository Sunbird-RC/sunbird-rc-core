package dev.sunbirdrc.registry.dao.impl;

import dev.sunbirdrc.pojos.APIMessage;
import dev.sunbirdrc.registry.config.GenericConfiguration;
import dev.sunbirdrc.registry.controller.RegistryTestBase;
import dev.sunbirdrc.registry.dao.RegistryDaoImpl;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.model.AuditRecordReader;
import dev.sunbirdrc.registry.model.DBConnectionInfoMgr;
import dev.sunbirdrc.registry.service.impl.EncryptionServiceImpl;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {RegistryDaoImpl.class, Environment.class, ObjectMapper.class, GenericConfiguration.class,
        EncryptionServiceImpl.class, APIMessage.class, DBConnectionInfoMgr.class})
@TestMethodOrder(MethodOrderer.MethodName.class)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
class RegistryDaoImplTest extends RegistryTestBase {
    private static Logger logger = LoggerFactory.getLogger(RegistryDaoImplTest.class);
    private static Graph graph;

    AuditRecordReader auditRecordReader;
    @Autowired
    private Environment environment;
    @Autowired
    private RegistryDaoImpl registryDao;
    @Value("${registry.context.base}")
    private String registryContext;

    @BeforeEach
    void initializeGraph() {
        graph = TinkerGraph.open();
    }

    @AfterEach
    void shutDown() throws Exception {
        if (graph != null) {
            graph.close();
        }
    }
}