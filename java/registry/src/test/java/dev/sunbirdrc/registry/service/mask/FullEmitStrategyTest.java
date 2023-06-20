package dev.sunbirdrc.registry.service.mask;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FullEmitStrategyTest {
    private FullEmitStrategy fullEmitStrategy;

    @Before
    public void setUp() throws Exception {
        fullEmitStrategy = new FullEmitStrategy();
    }

    @Test
    public void shouldEmitCompleteValueAsIs() {
        final String value = "testValue";
        final String expectedValue = "testValue";
        final String actualValue = fullEmitStrategy.updateValue(value);
        assertEquals(expectedValue, actualValue);
    }
}
