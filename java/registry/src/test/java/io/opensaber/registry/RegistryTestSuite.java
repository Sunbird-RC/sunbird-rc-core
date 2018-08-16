package io.opensaber.registry;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.opensaber.registry.dao.impl.EncryptionDaoImplTest;
import io.opensaber.registry.dao.impl.RegistryDaoImplTest;
import io.opensaber.registry.dao.impl.SearchDaoImplTest;
import io.opensaber.registry.service.impl.EncryptionServiceImplTest;
import io.opensaber.registry.service.impl.RegistryServiceImplTest;
import io.opensaber.registry.service.impl.SearchServiceImplTest;
import junit.framework.Test;
import junit.framework.TestSuite;

@SuiteClasses({RegistryDaoImplTest.class, RegistryServiceImplTest.class, 
	EncryptionDaoImplTest.class, EncryptionServiceImplTest.class, SearchServiceImplTest.class, SearchDaoImplTest.class})
@RunWith(Suite.class)
public class RegistryTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite(RegistryTestSuite.class.getName());
		//$JUnit-BEGIN$

		//$JUnit-END$
		return suite;
	}

}
