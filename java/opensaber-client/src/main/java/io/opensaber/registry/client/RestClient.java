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

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;

public class RestClient implements Client<String> {

    private static Logger logger = LoggerFactory.getLogger(RestClient.class);

    private HttpClient httpClient;
    private static Type mapType = new TypeToken<Map<String, Object>>(){}.getType();

    public RestClient() {
        this.httpClient = HttpClient.getInstance();
    }

    @Override
    public ResponseData<String> addEntity(RequestData<String> requestData, Map<String, String> headers) {
        System.out.println("Request Data = " + requestData.getRequestData());
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach((headerName, headerValue) -> httpHeaders.set(headerName, headerValue));
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ResponseSerializer.class, new ResponseSerializer());
        Gson gson = builder.create();
        Request request = new Request(new RequestParams(), gson.fromJson(requestData.getRequestData(), mapType));
        System.out.println("Request = " + gson.toJson(request));
        ResponseEntity<Response> response =
                httpClient.post(Configuration.BASE_URL + "/add", httpHeaders, gson.toJson(request));
        String result = gson.toJson(response.getBody());
        return new ResponseData<>(result);
    }

    @Override
    public ResponseData<String> updateEntity(RequestData<String> requestData, Map<String, String> headers) {
        return null;
    }

    @Override
    public ResponseData<String> addAndAssociateEntity(URI existingEntity, URI property,
                                                      RequestData<String> requestData, Map<String, String> headers) {
        return null;
    }

    @Override
    public ResponseData<String> readEntity(URI entity, Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach((headerName, headerValue) -> httpHeaders.set(headerName, headerValue));
        ResponseEntity<Response> response = httpClient.get(entity.toString(), httpHeaders);
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ResponseSerializer.class, new ResponseSerializer());
        Gson gson = builder.create();
        String result = gson.toJson(response.getBody());
        return new ResponseData<>(result);
    }
}
