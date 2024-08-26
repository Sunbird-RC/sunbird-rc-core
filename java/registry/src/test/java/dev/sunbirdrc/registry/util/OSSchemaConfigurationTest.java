package dev.sunbirdrc.registry.util;

import dev.sunbirdrc.views.FunctionDefinition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OSSchemaConfigurationTest {

    @Test
    void shouldReturnMatchingFunctionDefinition() {
        OSSchemaConfiguration osSchemaConfiguration = new OSSchemaConfiguration();
        osSchemaConfiguration.setFunctionDefinitions(Arrays.asList(
                FunctionDefinition.builder().name("func1").build(),
                FunctionDefinition.builder().name("func2").build()
        ));

        assertNotNull(osSchemaConfiguration.getFunctionDefinition("func1"));
        assertNotNull(osSchemaConfiguration.getFunctionDefinition("func2"));
    }

    @Test
    void shouldReturnNullForInvalidFunctionName() {
        OSSchemaConfiguration osSchemaConfiguration = new OSSchemaConfiguration();
        osSchemaConfiguration.setFunctionDefinitions(Arrays.asList(
                FunctionDefinition.builder().name("func1").build(),
                FunctionDefinition.builder().name("func2").build()
        ));

        assertNull(osSchemaConfiguration.getFunctionDefinition("func3"));
    }
}