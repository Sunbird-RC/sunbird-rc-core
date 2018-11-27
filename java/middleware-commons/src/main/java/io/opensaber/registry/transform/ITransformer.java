package io.opensaber.registry.transform;

import java.io.IOException;
import java.util.List;

public interface ITransformer<T> {

	public Data<T> transform(Data<Object> data) throws TransformationException, IOException;
	public void setPurgeData(List<String> keyToPruge);

}
