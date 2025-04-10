package dev.sunbirdrc.registry.service.mask;

import dev.sunbirdrc.registry.model.EventConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmitStrategyFactoryTest {
    @Test
    void shouldReturnHashStrategy() {
        IEmitStrategy emitStrategy = EmitStrategyFactory.getMaskConfig(EventConfig.HASH);
        assertTrue(emitStrategy instanceof HashEmitStrategy);
    }

    @Test
    void shouldReturnNoneStrategy() {
        IEmitStrategy emitStrategy = EmitStrategyFactory.getMaskConfig(EventConfig.NONE);
        assertTrue(emitStrategy instanceof NoneEmitStrategy);
    }

    @Test
    void shouldReturnFullStrategy() {
        IEmitStrategy emitStrategy = EmitStrategyFactory.getMaskConfig(EventConfig.FULL);
        assertTrue(emitStrategy instanceof FullEmitStrategy);
    }

    @Test
    void shouldReturnHashMaskStrategy() {
        IEmitStrategy emitStrategy = EmitStrategyFactory.getMaskConfig(EventConfig.HASH_MASK);
        assertTrue(emitStrategy instanceof HashMaskEmitStrategy);
    }

    @Test
    void shouldReturnMaskStrategy() {
        IEmitStrategy emitStrategy = EmitStrategyFactory.getMaskConfig(EventConfig.MASK);
        assertTrue(emitStrategy instanceof MaskEmitStrategy);
    }
}