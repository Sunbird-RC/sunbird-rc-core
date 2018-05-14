package io.opensaber.registry.client;

import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;

public class TransformationClient<T> {

    private RequestData<T> requestData;
    private TransformationConfiguration configuration;

    public TransformationClient(RequestData<T> requestData, TransformationConfiguration configuration) {
        this.requestData = requestData;
        this.configuration = configuration;
    }

    public ResponseData<T> transform() throws Exception {
        ResponseData<T> data = configuration.getTransformer().transform(requestData);
        return data;
    }
}
