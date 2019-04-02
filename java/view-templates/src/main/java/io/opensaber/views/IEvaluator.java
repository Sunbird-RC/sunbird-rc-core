package io.opensaber.views;

public interface IEvaluator<T> {
    /**
     * evaluates to provide result 
     * From a given expression, a provider class, a reference template of a sview template
     * @return
     */
    public T evaluate();
}
