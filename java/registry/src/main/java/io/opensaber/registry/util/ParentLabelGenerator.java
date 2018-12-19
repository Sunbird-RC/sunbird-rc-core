package io.opensaber.registry.util;

/**
 * For a given entity type, defines the parent label.
 */
public class ParentLabelGenerator {
    /**
     * This will be suffixed to each of the defined schema objects
     * as a parenter
     */
    private static String SUFFIX = "_GROUP";

    /**
     * Constructs the parent node label associated with the entityType
     * @param entityType
     * @return
     */
    public static String getLabel(String entityType) {
        return entityType + SUFFIX;
    }

    /**
     * Tells what is the parent label identifier.
     * @return
     */
    public static String getParentLabelId() {
        return SUFFIX;
    }
}
