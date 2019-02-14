package io.opensaber.registry.util;

/**
 * ReadConfigurator controls the data visible to the end user.
 */
public class ReadConfigurator {
    /**
     * Whether or not to include @type attributes
     * True, by default
     */
    private boolean includeTypeAttributes = true;

    /**
     * Whether or not to include encrypted properties
     * False, by default
     */
    private boolean includeEncryptedProp = false;

    /**
     * Expands the children objects on read
     * 1, by default
     */
    private int depth = 1;

    /**
     * Whether or not to include signatures
     * False, by default
     */
    private boolean includeSignatures = false;

    /**
     * Whether or not to include identifiers
     * True by default
     */
    private boolean includeIdentifiers = true;

    /**
     * Whether or not to include root identifiers
     * False, by default
     */
    private boolean includeRootIdentifiers = false;

    public boolean isIncludeTypeAttributes() {
        return includeTypeAttributes;
    }

    public void setIncludeTypeAttributes(boolean includeTypeAttributes) {
        this.includeTypeAttributes = includeTypeAttributes;
    }

    public boolean isIncludeEncryptedProp() {
        return includeEncryptedProp;
    }

    public void setIncludeEncryptedProp(boolean includeEncryptedProp) {
        this.includeEncryptedProp = includeEncryptedProp;
    }

    public boolean isIncludeSignatures() {
        return includeSignatures;
    }

    public void setIncludeSignatures(boolean includeSignatures) {
        this.includeSignatures = includeSignatures;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public boolean isIncludeIdentifiers() {
        return includeIdentifiers;
    }

    public void setIncludeIdentifiers(boolean includeIdentifiers) {
        this.includeIdentifiers = includeIdentifiers;
    }

    public boolean isIncludeRootIdentifiers() {
        return includeRootIdentifiers;
    }

    public void setIncludeRootIdentifiers(boolean includeRootIdentifiers) {
        this.includeRootIdentifiers = includeRootIdentifiers;
    }
}
