package io.opensaber.views;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;


public class FunctionEvaluator implements IEvaluator<Object>{

    private static final JexlEngine jexl = new JexlEngine();
    private JexlContext jexlContext = new MapContext();
    private FieldFunction function;
    private Expression jexlExpression;
    
    private static final String ARG = "arg";

    public FunctionEvaluator(FieldFunction function) {
        this.function = function;
    }

    public void setContextArgs() {
        int itr = 1;
        for (Object val : function.getArgValues()) {
            String arg = ARG + itr++;

            jexlContext.set(arg, val);
        }
    }

    private void prepare() {
        jexlExpression = jexl.createExpression(function.getExpression());
        setContextArgs();
    }

    @Override
    public Object evaluate() {
        prepare();
        Object result = jexlExpression.evaluate(jexlContext);

        return result;
    }

}
