package io.opensaber.registry.util;

/**
 * For a given entity type, defines the parent label.
 */
public class ParentLabelGenerator {
    private static String SUFFIX = "_GROUP";

    public static String getLabel(String entityType) {
        return entityType + SUFFIX;
    }
}
