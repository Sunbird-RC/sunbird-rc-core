package dev.sunbirdrc.registry.service.mask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaskEmitStrategyTest {
    private MaskEmitStrategy maskEmitStrategy;

    @BeforeEach
    public void setUp() throws Exception {
        maskEmitStrategy = new MaskEmitStrategy();
    }

    @Test
    public void shouldEmitMaskedValue() {
        final String value = "testValue";
        final String expectedValue = "XXXXValue";
        final String actualValue = maskEmitStrategy.updateValue(value);
        assertEquals(expectedValue, actualValue);
    }
}