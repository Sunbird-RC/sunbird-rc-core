package dev.sunbirdrc.registry.service.mask;

import dev.sunbirdrc.registry.model.EventConfig;

import java.util.HashMap;
import java.util.Map;

public class EmitMap {
    public static IEmitStrategy getMaskConfig(EventConfig config) {
        Map<EventConfig, IEmitStrategy> map = new HashMap<>();
        map.put(EventConfig.MASK, new MaskEmitStrategy());
        map.put(EventConfig.NONE, new NoneEmitStrategy());
        map.put(EventConfig.FULL, new FullEmitStrategy());
        map.put(EventConfig.HASH, new HashEmitStrategy());
        return map.get(config);
    }
}
