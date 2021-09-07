package io.opensaber.pojos.attestation.auto.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import io.opensaber.pojos.attestation.auto.PluginType;
import io.opensaber.pojos.attestation.exception.PolicyNotFoundException;

public class PluginFactory {
    public static PluginAdapter<JsonNode> getAdapter(PluginType type) throws PolicyNotFoundException {
        switch (type) {
            case AADHAR:
                return new AadharPluginAdapter();
            case LICENSE:
                return new LicensePluginAdapter();
            default:
                throw new PolicyNotFoundException("Type" + type +  "is not found");
        }
    }
}
