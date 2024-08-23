package dev.sunbirdrc.registry.service.mask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HashMaskEmitStrategyTest {
    private HashMaskEmitStrategy hashMaskEmitStrategy;

    @BeforeEach
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