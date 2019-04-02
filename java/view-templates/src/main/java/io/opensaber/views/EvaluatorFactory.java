package io.opensaber.views;

import java.util.List;

public class EvaluatorFactory {
    
    /**
     * returns the instance of IEvaluator implementations (like:FunctionEvaluator, ProviderEvaluator 
     * 
     * @param actualValues
     * @param functiondef
     * @return
     */
    public static IEvaluator<Object> getInstance(FunctionDefinition functiondef, List<Object> actualValues) {
        IEvaluator<Object> evaluator = null;
        FieldFunction function = null;

        if (functiondef.getResult() != null) {
            function = getFieldFunction(functiondef.getResult(), actualValues);
            evaluator = new FunctionEvaluator(function);

        } else if (functiondef.getProvider() != null) {
            function = getFieldFunction(functiondef.getProvider(), actualValues);
            evaluator = new ProviderEvaluator(function);
        } else if (functiondef.getReference() != null) {
            //TODO: implementation for reference evaluator 
        }

        return evaluator;
    }
    /**
     * Creates FieldFunction and sets argValues
     * @param expression
     * @param actualValues
     * @return
     */
    private static FieldFunction getFieldFunction(String expression, List<Object> actualValues) {
        FieldFunction function = new FieldFunction(expression);
        function.setArgValues(actualValues);
        return function;

    }
    
}
