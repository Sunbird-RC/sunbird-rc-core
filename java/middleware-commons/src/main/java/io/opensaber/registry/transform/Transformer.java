package io.opensaber.registry.transform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Transformer {

    private static final String EXCEPTION_MESSAGE = "Media type not supported";

    @Autowired
    private Json2LdTransformer json2LdTransformer;

    @Autowired
    private Json2JsonTransformer json2JsonTransformer;

    public ITransformer<Object> getInstance(Configuration config) throws TransformationException {
        ITransformer<Object> transformer = null;

        if (config == Configuration.JSON2LD) {
            transformer = json2LdTransformer;
        } else if (config == Configuration.JSON2JSON) {
            transformer = json2JsonTransformer;
        } else {
            throw new TransformationException(EXCEPTION_MESSAGE, ErrorCode.UNSUPPOTERTED_TYPE);

        }
        return transformer;
    }

}
