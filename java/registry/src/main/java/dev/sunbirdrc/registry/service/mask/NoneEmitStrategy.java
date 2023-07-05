package dev.sunbirdrc.registry.service.mask;

public class NoneEmitStrategy implements IEmitStrategy {
    @Override
    public String updateValue(String value) {
        return null;
    }
}
