package dev.sunbirdrc.registry.service.impl;


import dev.sunbirdrc.registry.app.SunbirdRCApplication;
import dev.sunbirdrc.registry.config.GenericConfiguration;
import dev.sunbirdrc.registry.controller.RegistryTestBase;
import dev.sunbirdrc.registry.dao.IRegistryDao;
import dev.sunbirdrc.registry.dao.SearchDao;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.service.RegistryService;
import dev.sunbirdrc.registry.service.ISearchService;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { SunbirdRCApplication.class, IRegistryDao.class, SearchDao.class, ISearchService.class,
		GenericConfiguration.class })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class NativeSearchServiceTest extends RegistryTestBase {

	private static final String BASE_SEARCH_JSONLD = "base_search_context.jsonld";
	private static final String CONTEXT_NAMESPACE = "http://example.com/voc/teacher/1.0.0/";
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	@Autowired
	private ISearchService searchService;
	@Autowired
	private RegistryService registryService;

	@Before
	public void initialize() throws IOException {

	}

}
