package io.opensaber.registry.service.impl;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.opensaber.registry.config.GenericConfiguration;
import io.opensaber.registry.dao.RegistryDao;
import io.opensaber.registry.dao.impl.RegistryDaoImpl;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.service.RegistryService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes={Environment.class,
		ObjectMapper.class,GenericConfiguration.class,RegistryDaoImpl.class,RegistryServiceImpl.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles(Constants.TEST_ENVIRONMENT)
public class RegistryServiceImplTest {
	
	@Autowired
	private RegistryDao registryDao;
	
	@Autowired
	private RegistryService registryService;
	
	@Test @Ignore
	public void testGetEntityById() throws RecordNotFoundException {
		registryService.getEntityById("1234");
	}

}
