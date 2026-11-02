package dev.sunbirdrc.registry.util;

/**
 * Labels helper for associated entity identifiers.
 */
public class RefLabelHelper {
    private static String SEPARATOR = "_";
    private static String ARRAY_SEPARATOR = "_arr_";

    /**
     * Prefix for edge labels formed for a declared entityReferences field
     * (dev.sunbirdrc.registry.util.OSSchemaConfiguration#entityReferences).
     * For readability/debugging only — NOT the safety check cascade-delete relies
     * on, since a field literally named "ref" can produce this same prefix via
     * getLabel's "field_id" shape (e.g. "ref_osid"). Use REFERENCE_EDGE_MARKER
     * (an edge property, not a label substring) to actually distinguish a
     * reference edge from a containment edge.
     */
    public static final String REFERENCE_EDGE_PREFIX = "ref_";

    /**
     * Edge property key marking an edge as a declared entity-reference (not
     * containment). Collision-proof unlike a label prefix: it is never derived
     * from a schema field name, so no field name can produce it by accident.
     */
    public static final String REFERENCE_EDGE_MARKER = "__osEntityReference__";

    /**
     * Generates the edge label for a declared entity-reference field.
     *
     * Qualified by the DECLARING entity type, not just the field name: SqlG stores every
     * edge sharing one label in a single table, with a separate nullable column-pair per
     * distinct (out-type, in-type) combination it has ever seen under that label. Two
     * different declaring entities using the same field name (e.g. Records.studentOsid and
     * Attendance.studentOsid, both -> Student) would otherwise share one label and one table,
     * and SqlG's generic inVertex()/outVertex() resolution does not reliably pick the right
     * column-pair for a given row in that case — observed in practice returning a bogus,
     * null-defaulted vertex (wrong type, id 0) instead of the real referenced vertex. A label
     * unique per declaring type keeps every label's edges to exactly one (out-type, in-type)
     * pair, which removes the ambiguity entirely.
     *
     * @param declaringEntityType - the entity type whose schema declares this field
     * @param field - the schema field name holding the reference
     * @return
     */
    public static String getReferenceEdgeLabel(String declaringEntityType, String field) {
        return REFERENCE_EDGE_PREFIX + declaringEntityType + SEPARATOR + field;
    }

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
