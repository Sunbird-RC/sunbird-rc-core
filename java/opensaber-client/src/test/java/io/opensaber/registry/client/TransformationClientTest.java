package io.opensaber.registry.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CharStreams;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.transform.JsonToJsonLDTransformer;
import static org.junit.Assert.*;
import org.junit.Test;


import java.io.InputStreamReader;

public class TransformationClientTest {

    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void test_client_transform() throws Exception {

        ObjectNode expectedTeacherJsonldOutput = (ObjectNode) mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_jsonld_output_data.json"))));
        JsonNode inputJson = mapper.readTree(CharStreams.toString(new InputStreamReader(
                JsonToJsonLDTransformer.class.getClassLoader().getResourceAsStream("teacher_json_input_data.json"))));

        RequestData<String> requestData = new RequestData<>(inputJson.toString());
        TransformationConfiguration configuration = TransformationConfiguration.builder()
                .transform(JsonToJsonLDTransformer.getInstance()).build();
        TransformationClient<String> client = new TransformationClient<>(requestData, configuration);
        ResponseData<String> response = client.transform();
        assertEquals(expectedTeacherJsonldOutput.toString(), response.getResponseData());
    }
}
