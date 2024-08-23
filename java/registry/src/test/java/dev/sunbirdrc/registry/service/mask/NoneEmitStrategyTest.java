package dev.sunbirdrc.registry.service.mask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NoneEmitStrategyTest {
    private NoneEmitStrategy noneEmitStrategy;

    @BeforeEach
    public void setUp() throws Exception {
        noneEmitStrategy = new NoneEmitStrategy();
    }

    @Test
    public void shouldNotEmitAnyValue() {
        final String value = "testValue";
        final String actualValue = noneEmitStrategy.updateValue(value);
        assertNull(actualValue);
    }
}