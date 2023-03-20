package dev.sunbirdrc.registry.service.mask;

import org.apache.commons.lang3.StringUtils;

public class MaskEmitStrategy implements IEmitStrategy {
    @Override
    public String updateValue(String value) {
        String replacement = StringUtils.repeat('X', value.length()/2);
        return value.replace(value.substring(0, value.length()/2), replacement);
    }
}
