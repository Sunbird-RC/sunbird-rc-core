package dev.sunbirdrc.claim.exception;

public class ClaimAlreadyProcessedException extends RuntimeException{
    public ClaimAlreadyProcessedException(String message) {
        super(message);
    }
}
