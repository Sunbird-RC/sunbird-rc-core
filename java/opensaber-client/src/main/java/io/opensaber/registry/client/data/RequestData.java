package io.opensaber.registry.client.data;

public class RequestData<T> {

    private final T requestData;

    public RequestData(T data) {
        this.requestData = data;
    }

    public T getRequestData() {
        return requestData;
    }
}
