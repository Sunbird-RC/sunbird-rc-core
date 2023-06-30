package dev.sunbirdrc.views;

import java.util.List;

public class EvaluatorFactory {

    /**
     * returns the instance of IEvaluator implementations (like:FunctionEvaluator, ProviderEvaluator
     *
     * @param functiondef
     * @param actualValues
     * @param argumentsPath
     * @return
     */
    public static IEvaluator<Object> getInstance(FunctionDefinition functiondef, List<Object> actualValues, String[] argumentsPath) {
        IEvaluator<Object> evaluator = null;
        FieldFunction function = null;

        if (functiondef.getResult() != null) {
            function = getFieldFunction(functiondef.getResult(), actualValues, argumentsPath);
            evaluator = new FunctionEvaluator(function);

        } else if (functiondef.getProvider() != null) {
            function = getFieldFunction(functiondef.getProvider(), actualValues, argumentsPath);
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
     * @param argumentsPath
     * @return
     */
    private static FieldFunction getFieldFunction(String expression, List<Object> actualValues, String[] argumentsPath) {
        FieldFunction function = new FieldFunction(expression);
        function.setArgValues(actualValues);
        function.setArgumentsPaths(argumentsPath);
        return function;

    }

}
