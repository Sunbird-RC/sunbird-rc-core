package dev.sunbirdrc.pojos.attestation.auto.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import dev.sunbirdrc.pojos.attestation.auto.PluginType;
import dev.sunbirdrc.pojos.attestation.exception.PolicyNotFoundException;

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
