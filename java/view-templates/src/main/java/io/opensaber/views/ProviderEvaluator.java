package io.opensaber.views;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProviderEvaluator implements IEvaluator<Object> {

    private static Logger logger = LoggerFactory.getLogger(ProviderEvaluator.class);

    private FieldFunction function;

    public ProviderEvaluator(FieldFunction function) {
        this.function = function;
    }

    @Override
    public Object evaluate() {
        IViewFunctionProvider<Object> viewFuntionProvider = getInstance(function.getExpression());
        Object result = viewFuntionProvider.doAction(function.getArgValues());
        return result;
    }

    /**
     * invokes instance for given providerName
     * @param providerName    full qualified name of class
     * @return
     */
    public IViewFunctionProvider<Object> getInstance(String providerName) {

        IViewFunctionProvider<Object> viewFunctionProvider = null;
        try {
            if (providerName == null || providerName.isEmpty()) {
                throw new IllegalArgumentException(
                        "view function provider class {} cannot be instantiate with empty value");
            }
            Class<?> advisorClass = Class.forName(providerName);
            viewFunctionProvider = (IViewFunctionProvider) advisorClass.newInstance();
            logger.info("Invoked view function provider class with classname: " + providerName);

        } catch (ClassNotFoundException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException e) {
            logger.error("view function provider class {} cannot be instantiate with exception:", providerName, e);
        }
        return viewFunctionProvider;
    }

}
