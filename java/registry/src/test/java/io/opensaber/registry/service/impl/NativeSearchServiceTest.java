package io.opensaber.registry.service.impl;


import io.opensaber.registry.app.OpenSaberApplication;
import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.controller.RegistryTestBase;
import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.dao.SearchDao;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RegistryService;
import io.opensaber.registry.service.ISearchService;
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
@SpringBootTest(classes = { OpenSaberApplication.class, IRegistryDao.class, SearchDao.class, ISearchService.class,
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
