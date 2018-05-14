package io.opensaber.registry.client;

import com.google.common.base.Preconditions;
import io.opensaber.registry.transform.ITransformer;

public class TransformationConfiguration {
    private ITransformer transformer;

    private TransformationConfiguration(Builder builder) {
        Preconditions.checkNotNull(builder.transformer);
        this.transformer = builder.transformer;
    }

    public ITransformer getTransformer() {
        return transformer;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ITransformer transformer;

        private Builder(){}

        public Builder transform(ITransformer transformer) {
            this.transformer = transformer;
            return this;
        }

        public TransformationConfiguration build() {
            return new TransformationConfiguration(this);
        }

    }
}
