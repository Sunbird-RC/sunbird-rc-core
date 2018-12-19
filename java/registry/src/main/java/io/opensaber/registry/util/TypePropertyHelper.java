package io.opensaber.registry.util;

/**
 * TypePropertyHelper will hold functions related to the internal, LD-centric
 * @type definition.
 */
public class TypePropertyHelper {
    private static String RDF_TYPE = "@type";

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
