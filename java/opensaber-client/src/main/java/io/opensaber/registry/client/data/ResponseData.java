package io.opensaber.registry.client.data;

public class ResponseData<T> {

    private final T responseData;

    public ResponseData(T data) {
        this.responseData = data;
    }

    public T getResponseData() {
        return responseData;
    }
}
