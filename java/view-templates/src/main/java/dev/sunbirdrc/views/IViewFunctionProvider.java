package dev.sunbirdrc.views;

import java.util.List;

public interface IViewFunctionProvider<T> {

	/**
	 * A list of objects (ordered) are passed to the function. arg1 will appear first in the list, followed by arg2 and so on.
	 * The adopter can choose to implement the transformation.
	 * @param values
	 * @return
	 */
	T doAction(List<Object> values);
    T doAction(List<Object> values, String[] paths);

}
