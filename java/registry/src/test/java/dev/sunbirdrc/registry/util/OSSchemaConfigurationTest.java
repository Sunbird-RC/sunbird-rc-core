package dev.sunbirdrc.registry.util;

import dev.sunbirdrc.views.FunctionDefinition;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class OSSchemaConfigurationTest{
	@Test
	public void shouldReturnMatchingFunctionDefinition() {
		OSSchemaConfiguration osSchemaConfiguration = new OSSchemaConfiguration();
		osSchemaConfiguration.setFunctionDefinitions(Arrays.asList(
				FunctionDefinition.builder().name("func1").build(),
				FunctionDefinition.builder().name("func2").build()
		));

		Assert.assertNotNull(osSchemaConfiguration.getFunctionDefinition("func1"));
		Assert.assertNotNull(osSchemaConfiguration.getFunctionDefinition("func2"));
	}
	@Test
	public void shouldReturnNullForInvalidFunctionName() {
		OSSchemaConfiguration osSchemaConfiguration = new OSSchemaConfiguration();
		osSchemaConfiguration.setFunctionDefinitions(Arrays.asList(
				FunctionDefinition.builder().name("func1").build(),
				FunctionDefinition.builder().name("func2").build()
		));

		Assert.assertNull(osSchemaConfiguration.getFunctionDefinition("func3"));
	}

}