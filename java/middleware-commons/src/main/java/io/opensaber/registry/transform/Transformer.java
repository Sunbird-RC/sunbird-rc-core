package io.opensaber.registry.transform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Transformer {

    private static final String EXCEPTION_MESSAGE = "Media type not suppoted";

    @Autowired
    private Json2LdTransformer json2LdTransformer;

    @Autowired
    private Ld2JsonTransformer ld2JsonTransformer;

    @Autowired
    private Ld2LdTransformer Ld2LdTransformer;

    public ITransformer<Object> getInstance(Configuration config) throws TransformationException {
        ITransformer<Object> transformer = null;

        if (config == Configuration.JSON2LD) {
            transformer = json2LdTransformer;
        } else if (config == Configuration.LD2JSON) {
            transformer = ld2JsonTransformer;
        } else if (config == Configuration.LD2LD) {
            transformer = Ld2LdTransformer;
        } else {
            throw new TransformationException(EXCEPTION_MESSAGE, ErrorCode.UNSUPPOTERTED_TYPE);

        }
        return transformer;
    }

}
