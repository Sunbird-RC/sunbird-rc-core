package dev.sunbirdrc.registry.service.mask;

import dev.sunbirdrc.registry.model.EventConfig;

import java.util.HashMap;
import java.util.Map;

public class EmitMap {
    public static IEmitStrategy getMaskConfig(EventConfig config) {
        switch (config) {
            case MASK:
                return new MaskEmitStrategy();
            case NONE:
                return new NoneEmitStrategy();
            case FULL:
                return new FullEmitStrategy();
            case HASH:
                return new HashEmitStrategy();
            case HASH_MASK:
                return new HashMaskEmitStrategy();
            default:
                throw new IllegalArgumentException(config.name() + " not supported type of emit config");
        }
    }
}
