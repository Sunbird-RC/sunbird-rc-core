package io.opensaber.registry.operators;

import io.opensaber.pojos.APIMessage;

@FunctionalInterface
public interface Operator {
    String execute(APIMessage apiMessage) throws Exception;
}
