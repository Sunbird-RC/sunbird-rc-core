package dev.sunbirdrc.registry.service.mask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class NoneEmitStrategyTest {
    private NoneEmitStrategy noneEmitStrategy;

    @BeforeEach
    void setUp() throws Exception {
        noneEmitStrategy = new NoneEmitStrategy();
    }

    @Test
    void shouldNotEmitAnyValue() {
        final String value = "testValue";
        final String actualValue = noneEmitStrategy.updateValue(value);
        assertNull(actualValue);
    }
}