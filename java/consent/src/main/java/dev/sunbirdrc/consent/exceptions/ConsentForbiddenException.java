package dev.sunbirdrc.consent.exceptions;

public class ConsentForbiddenException extends Exception {
    public ConsentForbiddenException(String consentTimeExpired) {
        super(consentTimeExpired);
    }
}
