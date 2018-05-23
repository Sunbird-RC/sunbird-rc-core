package io.opensaber.registry.transform;

import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.exception.TransformationException;

public interface ITransformer<T> {

    ResponseData<T> transform(RequestData<T> data) throws TransformationException;

}
