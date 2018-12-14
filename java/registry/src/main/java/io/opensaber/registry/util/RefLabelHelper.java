package io.opensaber.registry.util;

public class RefLabelHelper {
    private static String SEPARATOR = "_";

    public static String getLabel(String referenceName, String id) {
        return referenceName + SEPARATOR + id;
    }

    public static boolean isRefLabel(String lbl, String id) {
        return lbl.contains(SEPARATOR) && lbl.contains(id);
    }

    public static String getRefEntityName(String label) {
        return label.substring(0, label.indexOf(SEPARATOR));
    }
}
