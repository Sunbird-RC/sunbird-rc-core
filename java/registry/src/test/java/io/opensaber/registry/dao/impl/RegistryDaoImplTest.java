package io.opensaber.registry.dao.impl;

import io.opensaber.pojos.APIMessage;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.dao.RegistryDaoImpl;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.model.AuditRecordReader;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import io.opensaber.registry.service.impl.EncryptionServiceImpl;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.junit.After;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { RegistryDaoImpl.class, Environment.class, ObjectMapper.class, GenericConfiguration.class,
		EncryptionServiceImpl.class, APIMessage.class, DBConnectionInfoMgr.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryDaoImplTest extends RegistryTestBase {
	private static Logger logger = LoggerFactory.getLogger(RegistryDaoImplTest.class);
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

	AuditRecordReader auditRecordReader;
	@Autowired
	private Environment environment;
	@Autowired
	private RegistryDaoImpl registryDao;
    @Value("${registry.context.base}")
	private String registryContext;

	@Before
	public void initializeGraph() {
		graph = TinkerGraph.open();
	}


	@After
	public void shutDown() throws Exception {
		if (graph != null) {
			graph.close();
		}
	}





}
