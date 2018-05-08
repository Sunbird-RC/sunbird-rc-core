package io.opensaber.registry.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.opensaber.pojos.Request;
import io.opensaber.pojos.RequestParams;
import io.opensaber.pojos.Response;
import io.opensaber.pojos.ResponseSerializer;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.config.Configuration;
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

public class RestClient implements Client<String> {

    private static Logger logger = LoggerFactory.getLogger(RestClient.class);

    private HttpClient httpClient;
    private Gson gson;
    private static Type mapType = new TypeToken<Map<String, Object>>(){}.getType();

    public RestClient() {
        this.httpClient = HttpClient.getInstance();
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ResponseSerializer.class, new ResponseSerializer());
        gson = builder.create();
    }

    @Override
    public ResponseData<String> addEntity(RequestData<String> requestData, Map<String, String> headers) {
        ResponseEntity<Response> response =
                httpClient.post(Configuration.BASE_URL + "/add", createHttpHeaders(headers), gson.toJson(createRequestEntity(requestData)));
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
    public ResponseData<String> readEntity(URI entity, Map<String, String> headers) {
        ResponseEntity<Response> response = httpClient.get(entity.toString(), createHttpHeaders(headers));
        String result = gson.toJson(response.getBody());
        return new ResponseData<>(result);
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
