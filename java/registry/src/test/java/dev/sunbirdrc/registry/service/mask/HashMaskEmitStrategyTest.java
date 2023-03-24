package dev.sunbirdrc.registry.service.mask;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HashMaskEmitStrategyTest {
    private HashMaskEmitStrategy hashMaskEmitStrategy;

    @Before
    public void setUp() throws Exception {
        hashMaskEmitStrategy = new HashMaskEmitStrategy();
    }

    @Test
    public void shouldEmitHashedMaskedValue() {
        final String value = "testValue";
        final String actualValue = hashMaskEmitStrategy.updateValue(value);
        boolean isEndCorrect = actualValue.endsWith("XXXXValue");
        assertTrue(isEndCorrect);
        assertNotEquals(actualValue, value);
    }
}
