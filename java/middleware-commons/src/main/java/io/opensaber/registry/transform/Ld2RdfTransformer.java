package io.opensaber.registry.transform;

import java.io.IOException;
import java.util.List;

import org.apache.jena.rdf.model.Model;

import io.opensaber.registry.middleware.util.Constants;
import io.opensaber.registry.middleware.util.RDFUtil;

public class Ld2RdfTransformer implements ITransformer<Object> {
	
	@Override
	public Data<Object> transform(Data<Object> data) throws TransformationException, IOException {
		Model rdfModel = RDFUtil.getRdfModelBasedOnFormat(data.getData().toString(), Constants.JENA_LD_FORMAT);		
		return new Data<Object>(rdfModel);
	}

	@Override
	public void setPurgeData(List<String> keyToPruge) {
		// Nothing to purge
		
	}

}
