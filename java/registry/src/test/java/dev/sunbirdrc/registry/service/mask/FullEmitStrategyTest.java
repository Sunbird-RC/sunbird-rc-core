package dev.sunbirdrc.registry.service.mask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FullEmitStrategyTest {
    private FullEmitStrategy fullEmitStrategy;

    @BeforeEach
     void setUp() throws Exception {
        fullEmitStrategy = new FullEmitStrategy();
    }

    @Test
     void shouldEmitCompleteValueAsIs() {
        final String value = "testValue";
        final String expectedValue = "testValue";
        final String actualValue = fullEmitStrategy.updateValue(value);
        assertEquals(expectedValue, actualValue);
    }
}