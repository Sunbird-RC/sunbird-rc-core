package io.opensaber.claim.model;

public enum ClaimStatus {
    OPEN,
    CLOSED;

    public static String getOpen() {
        return OPEN.toString();
    }

    public static String getClosed() {
        return CLOSED.toString();
    }
}
