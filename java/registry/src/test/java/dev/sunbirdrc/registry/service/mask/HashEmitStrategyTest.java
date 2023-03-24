package dev.sunbirdrc.registry.service.mask;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class HashEmitStrategyTest {
    private HashEmitStrategy hashEmitStrategy;

    @Before
    public void setUp() throws Exception {
        hashEmitStrategy = new HashEmitStrategy();
    }

    @Test
    public void shouldEmitHashedValue() {
        final String value = "testValue";
        final String actualValue = hashEmitStrategy.updateValue(value);
        assertNotEquals(actualValue, value);
    }
}
