package dev.sunbirdrc.provider;

import dev.sunbirdrc.views.IViewFunctionProvider;
import java.util.List;
/**
 * This class is a sample implementation class of IViewFunctionProvider<T>
 *
 */
public class SampleViewFunctionProvider implements IViewFunctionProvider<String> {

    @Override
    public String doAction(List<Object> values) {
        // doing a simple concat for the values
        return concat(values);
    }

    @Override
    public String doAction(List<Object> values, String[] paths) {
        return doAction(values);
    }

    /**
     * simple concat for the values as string and comma(',') as seperator
     *
     * @param args
     * @return
     */
    public String concat(List<Object> args) {
        String res = "";
        for (Object arg : args) {
            res = res.toString().isEmpty() ? arg.toString() : (res + " : " + arg.toString());
        }
        return res;
    }

}
