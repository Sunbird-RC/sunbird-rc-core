package dev.sunbirdrc.views;

import java.util.List;

public class FieldFunction {

    private String expression;
    private List<Object> argValues;
    private String[] argumentsPath;

    public FieldFunction(String expression) {
        this.expression = expression;
    }

    public void setArgValues(List<Object> args) {
        this.argValues = args;
    }

    public String getExpression() {
        return this.expression;
    }

    public List<Object> getArgValues() {
        return this.argValues;
    }

    public void setArgumentsPaths(String[] argumentsPath) {
        this.argumentsPath = argumentsPath;
    }

    public String[] getArgumentsPath() {
        return argumentsPath;
    }
}
