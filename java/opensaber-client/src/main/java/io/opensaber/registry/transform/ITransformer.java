package io.opensaber.registry.transform;

import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;

public interface ITransformer<T> {

    ResponseData<T> transform(RequestData<T> data) throws Exception;

}
