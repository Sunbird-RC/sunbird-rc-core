package io.opensaber.registry.util;

import io.opensaber.registry.middleware.util.Constants;

/**
 * TypePropertyHelper will hold functions related to the internal, LD-centric
 * @type definition.
 */
public class TypePropertyHelper {
    private static String RDF_TYPE = Constants.TYPE_STR_JSON_LD;

    public static String getTypeName() {
        return RDF_TYPE;
    }

    /**
     * Identifies whether the passed in property is type related
     * @param propertyName
     * @return
     */
    public static boolean isTypeProperty(String propertyName) {
        return propertyName.equals(RDF_TYPE);
    }
}
