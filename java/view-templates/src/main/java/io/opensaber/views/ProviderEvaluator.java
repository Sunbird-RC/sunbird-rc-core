package io.opensaber.views;

public class ProviderEvaluator implements IEvaluator<Object> {

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
                        "view function provider class cannot be instantiate with empty value");
            }
            Class<?> advisorClass = Class.forName(providerName);
            viewFunctionProvider = (IViewFunctionProvider) advisorClass.newInstance();

        } catch (ClassNotFoundException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException e) {
            throw new IllegalArgumentException("view function provider cannot be instantiated");
        }
        return viewFunctionProvider;
    }

}
