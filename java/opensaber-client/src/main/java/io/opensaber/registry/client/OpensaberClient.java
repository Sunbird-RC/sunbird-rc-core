package io.opensaber.registry.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.opensaber.pojos.Request;
import io.opensaber.pojos.RequestParams;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseSerializer;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.config.Configuration;
import io.opensaber.registry.transform.ITransformer;
import io.opensaber.registry.transform.JsonldToJsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class OpensaberClient implements Client<String> {

    private static Logger logger = LoggerFactory.getLogger(OpensaberClient.class);
    public static Builder builder() {
        return new Builder();
    }

    private ITransformer<String> requestTransformer;
    private ITransformer<String> responseTransformer;

    private HttpClient httpClient;
    private Gson gson;
    private ObjectMapper mapper = new ObjectMapper();
    private static Type mapType = new TypeToken<Map<String, Object>>(){}.getType();



    private OpensaberClient(Builder clientBuilder) {
        Preconditions.checkNotNull(clientBuilder.requestTransformer);
        Preconditions.checkNotNull(clientBuilder.responseTransformer);
        this.requestTransformer = clientBuilder.requestTransformer;
        this.responseTransformer = clientBuilder.responseTransformer;

        this.httpClient = HttpClient.getInstance();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ResponseSerializer.class, new ResponseSerializer());
        gson = gsonBuilder.create();
    }



    public static final class Builder {
        private ITransformer<String> requestTransformer;
        private ITransformer<String> responseTransformer;

        public Builder requestTransformer(ITransformer<String> requestTransformer) {
            this.requestTransformer = requestTransformer;
            return this;
        }

        public Builder responseTransformer(ITransformer<String> responseTransformer) {
            this.responseTransformer = responseTransformer;
            return this;
        }

        public OpensaberClient build() {
            return new OpensaberClient(this);
        }
    }


    @Override
    public ResponseData<String> addEntity(RequestData<String> requestData, Map<String, String> headers) throws Exception {
        ResponseData<String> transformedReqData = requestTransformer.transform(requestData);
        logger.info("AddEntity Transformed Request Data: " + transformedReqData.getResponseData());
        System.out.println("AddEntity Transformed Request Data: " + transformedReqData.getResponseData());
        ResponseEntity<Response> response =
                httpClient.post(Configuration.BASE_URL + "/add", createHttpHeaders(headers),
                        gson.toJson(createRequestEntity(new RequestData<>(transformedReqData.getResponseData()))));
        String result = gson.toJson(response.getBody());
        return new ResponseData<>(result);
    }

    @Override
    public ResponseData<String> updateEntity(RequestData<String> requestData, Map<String, String> headers) {
        ResponseEntity<Response> response =
                httpClient.post(Configuration.BASE_URL + "/update",
                        createHttpHeaders(headers), gson.toJson(createRequestEntity(requestData)));
        String result = gson.toJson(response.getBody());
        return new ResponseData<>(result);
    }

    @Override
    public ResponseData<String> addAndAssociateEntity(URI existingEntity, URI property,
                                                      RequestData<String> requestData, Map<String, String> headers)
            throws UnsupportedEncodingException {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", URLEncoder.encode(existingEntity.toString(), "UTF-8"));
        queryParams.put("prop", URLEncoder.encode(property.toString(), "UTF-8"));
        ResponseEntity<Response> response =
                httpClient.post(Configuration.BASE_URL + "/add",
                        createHttpHeaders(headers), queryParams, gson.toJson(createRequestEntity(requestData)));
        String result = gson.toJson(response.getBody());
        return new ResponseData<>(result);
    }

    @Override
    public ResponseData<String> readEntity(URI entity, Map<String, String> headers) throws Exception {
        ResponseEntity<Response> response = httpClient.get(entity.toString(), createHttpHeaders(headers));
        System.out.println(gson.toJson(response.getBody()));
        JsonObject responseJson = gson.toJsonTree(response.getBody()).getAsJsonObject();
        String resultNode = gson.toJson(response.getBody().getResult(), mapType);
        String transformedJson = JsonldToJsonTransformer.getInstance().transform(new RequestData<>(resultNode)).getResponseData();
        logger.info("Transformed JSON = " + transformedJson);
        System.out.println("Transformed JSON = " + transformedJson);
        responseJson.add("result", gson.fromJson(transformedJson, JsonObject.class));
        // String result = gson.toJson(response.getBody());
        return new ResponseData<>(responseJson.toString());
    }

    private Request createRequestEntity(RequestData<String> requestData) {
        return new Request(new RequestParams(), gson.fromJson(requestData.getRequestData(), mapType));
    }

    private HttpHeaders createHttpHeaders(Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach((headerName, headerValue) -> httpHeaders.set(headerName, headerValue));
        return httpHeaders;
    }
}
