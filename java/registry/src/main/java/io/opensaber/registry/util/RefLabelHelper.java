package io.opensaber.registry.util;

/**
 * Labels helper for associated entity identifiers.
 */
public class RefLabelHelper {
    private static String SEPARATOR = "_";
    private static String ARRAY_SEPARATOR = "_arr_";

    /**
     * Generates the key that could be persisted for a given reference
     * and id
     * @param referenceName - the other reference
     * @param id - the id to suffix
     * @return
     */
    public static String getLabel(String referenceName, String id) {
        return referenceName + SEPARATOR + id;
    }

    /**
     * Generates the key that could be persisted for a given reference
     * and id
     * @param referenceName - the other reference
     * @param id - the id to suffix
     * @return
     */
    public static String getArrayLabel(String referenceName, String id) {
        return referenceName + ARRAY_SEPARATOR + id;
    }

    /**
     * Given a label and id, identifies if the label was
     * generated using this class.
     * @param lbl
     * @param id
     * @return
     */
    public static boolean isArrayLabel(String lbl, String id) {
        return lbl.contains(ARRAY_SEPARATOR) && lbl.contains(id);
    }

    /**
     * Given a label and id, identifies if the label was
     * generated using this class.
     * @param lbl
     * @param id
     * @return
     */
    public static boolean isRefLabel(String lbl, String id) {
        return lbl.contains(SEPARATOR) && lbl.contains(id);
    }

    /**
     * Given a label read from the database, identifies what is the
     * reference to which this points to.
     * @param label
     * @return
     */
    public static String getRefEntityName(String label) {
        return label.substring(0, label.indexOf(SEPARATOR));
    }

    /**
     * Identifies if the label belongs to a parent group
     * @param label
     * @return
     */
    public static boolean isParentLabel(String label) {
        return label.contains(ParentLabelGenerator.getParentLabelId());
    }
}
