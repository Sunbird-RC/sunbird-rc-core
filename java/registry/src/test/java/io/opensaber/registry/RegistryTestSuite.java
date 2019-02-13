package io.opensaber.registry;

import com.googlecode.junittoolbox.SuiteClasses;
import com.googlecode.junittoolbox.WildcardPatternSuite;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;

@RunWith(WildcardPatternSuite.class)
@SuiteClasses("**/*Test.class")
public class RegistryTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite(RegistryTestSuite.class.getName());
		// $JUnit-BEGIN$

		// $JUnit-END$
		return suite;
	}

}
