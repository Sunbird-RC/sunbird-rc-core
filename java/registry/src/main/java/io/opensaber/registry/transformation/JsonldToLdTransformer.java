package io.opensaber.registry.transformation;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opensaber.registry.middleware.transform.Data;
import io.opensaber.registry.middleware.transform.ITransformer;
import io.opensaber.registry.middleware.transform.TransformationException;

@Component
public class JsonldToLdTransformer implements ITransformer<Object> {

	@Override
	public Data<Object> transform(Data<Object> data) throws TransformationException, IOException {
		JsonNode input = new ObjectMapper().readTree(data.getData().toString());
		return new Data<>(input);
	}

	@Override
	public void setPurgeData(List<String> keyToPruge) {

	}

}
