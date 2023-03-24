package dev.sunbirdrc.registry.service.mask;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NoneEmitStrategyTest {
    private NoneEmitStrategy noneEmitStrategy;

    @Before
    public void setUp() throws Exception {
        noneEmitStrategy = new NoneEmitStrategy();
    }

    @Test
    public void shouldEmitCompleteValueAsIs() {
        final String value = "testValue";
        final String actualValue = noneEmitStrategy.updateValue(value);
        assertNull(actualValue);
    }
}
