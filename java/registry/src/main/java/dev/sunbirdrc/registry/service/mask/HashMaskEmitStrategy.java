package dev.sunbirdrc.registry.service.mask;

public class HashMaskEmitStrategy implements IEmitStrategy {
    @Override
    public String updateValue(String value) {
        return new HashEmitStrategy().updateValue(value) + "-" + new MaskEmitStrategy().updateValue(value);
    }
}
