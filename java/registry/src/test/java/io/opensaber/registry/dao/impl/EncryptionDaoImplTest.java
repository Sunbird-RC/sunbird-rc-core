package io.opensaber.registry.dao.impl;

import com.google.gson.Gson;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.AuditRecordReader;
import io.opensaber.registry.service.impl.EncryptionServiceImpl;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Environment.class, ObjectMapper.class, GenericConfiguration.class,
		EncryptionServiceImpl.class, AuditRecordReader.class })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class EncryptionDaoImplTest extends RegistryTestBase {
	private static Logger logger = LoggerFactory.getLogger(EncryptionDaoImplTest.class);
	private static Graph graph;
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	@Rule
	public TestRule watcher = new TestWatcher() {
		@Override
		protected void starting(Description description) {
			logger.debug("Executing test: " + description.getMethodName());
		}

		@Override
		protected void succeeded(Description description) {
			logger.debug("Successfully executed test: " + description.getMethodName());
		}

		@Override
		protected void failed(Throwable e, Description description) {
			logger.debug(
					String.format("Test %s failed. Error message: %s", description.getMethodName(), e.getMessage()));
		}
	};
	@Autowired
	AuditRecordReader auditRecordReader;

	/*
	 * @Mock private SchemaConfigurator mockSchemaConfigurator;
	 */
	@Autowired
	private IRegistryDao registryDao;
	@Autowired
	private Gson gson;
	@Mock
	private EncryptionServiceImpl encryptionMock;

	@Value("${encryption.enabled}")
	private boolean encryptionEnabled;

	@Before
	public void initializeGraph() throws IOException {
	    auditRecordReader = new AuditRecordReader(databaseProvider);
		Assume.assumeTrue(encryptionEnabled);
	}

	public void closeDB() throws Exception {
		databaseProvider.shutdown();
	}

	@After
	public void shutDown() throws Exception {
		if (graph != null) {
			graph.close();
		}
	}

	public Vertex getVertexForSubject(String subjectValue, String property, String objectValue) {
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

	public Vertex getVertexWithMultipleProperties(String subjectValue, Map<String, Object> map) {
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
