package io.opensaber.actors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.opensaber.pojos.attestation.auto.AutoAttestationMessage;
import io.opensaber.pojos.attestation.auto.AutoAttestationPolicy;
import io.opensaber.pojos.attestation.auto.PluginType;
import io.opensaber.pojos.attestation.auto.adapter.PluginAdapter;
import io.opensaber.pojos.attestation.auto.adapter.PluginFactory;
import org.springframework.http.ResponseEntity;
import org.sunbird.akka.core.BaseActor;
import org.sunbird.akka.core.MessageProtos;

import java.util.List;

public class AutoAttestorActor extends BaseActor {
    @Override
    protected void onReceive(MessageProtos.Message request) throws Throwable {
        String payLoad = request.getPayload().getStringValue();

        AutoAttestationMessage autoAttestationMessage = new ObjectMapper().readValue(payLoad,
                AutoAttestationMessage.class);
        AutoAttestationPolicy autoAttestationPolicy = autoAttestationMessage.getAutoAttestationPolicy();

        String typePath = autoAttestationPolicy.getTypePath();
        String valuePath = autoAttestationPolicy.getValuePath();
        JsonNode input = autoAttestationMessage.getInput();

        PluginType type = PluginType.valueOf(readValFromJsonTree(typePath, input));
        PluginAdapter<JsonNode> adapter = PluginFactory.getAdapter(type);
        String value = readValFromJsonTree(valuePath, input);
        JsonNode  requestBody = buildRequestBody(value);
        ResponseEntity<JsonNode> responseEntity = adapter.execute(requestBody);
        if(responseEntity.getStatusCode().is2xxSuccessful()) {
           // mark as attested
        } else {
            // mark as failed
        }
    }

    private JsonNode buildRequestBody(String value) {
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("value", value);
        return objectNode;
    }

    private String readValFromJsonTree(String path, JsonNode input) {
        Configuration alwaysReturnListConfig = Configuration.builder().options(Option.ALWAYS_RETURN_LIST).build();
        List<String> typeList = JsonPath.using(alwaysReturnListConfig).parse(input).read(path);
        return typeList.get(0);
    }
}
