package dev.sunbirdrc.registry.service.mask;

import dev.sunbirdrc.registry.model.EventConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmitStrategyFactoryTest {
    @Test
    public void shouldReturnHashStrategy() {
        IEmitStrategy emitStrategy = EmitStrategyFactory.getMaskConfig(EventConfig.HASH);
        assertTrue(emitStrategy instanceof HashEmitStrategy);
    }

    @Test
    public void shouldReturnNoneStrategy() {
        IEmitStrategy emitStrategy = EmitStrategyFactory.getMaskConfig(EventConfig.NONE);
        assertTrue(emitStrategy instanceof NoneEmitStrategy);
    }

    @Test
    public void shouldReturnFullStrategy() {
        IEmitStrategy emitStrategy = EmitStrategyFactory.getMaskConfig(EventConfig.FULL);
        assertTrue(emitStrategy instanceof FullEmitStrategy);
    }

    @Test
    public void shouldReturnHashMaskStrategy() {
        IEmitStrategy emitStrategy = EmitStrategyFactory.getMaskConfig(EventConfig.HASH_MASK);
        assertTrue(emitStrategy instanceof HashMaskEmitStrategy);
    }

    @Test
    public void shouldReturnMaskStrategy() {
        IEmitStrategy emitStrategy = EmitStrategyFactory.getMaskConfig(EventConfig.MASK);
        assertTrue(emitStrategy instanceof MaskEmitStrategy);
    }
}