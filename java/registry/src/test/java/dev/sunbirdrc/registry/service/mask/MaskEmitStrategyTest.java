package dev.sunbirdrc.registry.service.mask;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MaskEmitStrategyTest {
    private MaskEmitStrategy maskEmitStrategy;

    @Before
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
