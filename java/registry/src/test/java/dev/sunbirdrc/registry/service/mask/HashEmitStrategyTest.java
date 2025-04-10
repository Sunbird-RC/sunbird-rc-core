package dev.sunbirdrc.registry.service.mask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HashEmitStrategyTest {
    private HashEmitStrategy hashEmitStrategy;

    @BeforeEach
    void setUp() throws Exception {
        hashEmitStrategy = new HashEmitStrategy();
    }

    @Test
    void shouldEmitHashedValue() {
        final String value = "testValue";
        final String actualValue = hashEmitStrategy.updateValue(value);
        assertNotEquals(actualValue, value);
    }
}