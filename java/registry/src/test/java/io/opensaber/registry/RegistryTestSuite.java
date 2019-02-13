package io.opensaber.registry;

import io.opensaber.registry.service.impl.EncryptionServiceImplTest;
import io.opensaber.registry.service.impl.SignatureServiceImplTest;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({
		EncryptionServiceImplTest.class, SignatureServiceImplTest.class})
@RunWith(Suite.class)
public class RegistryTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite(RegistryTestSuite.class.getName());
		// $JUnit-BEGIN$

		// $JUnit-END$
		return suite;
	}

}
