package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class KeycloakUserException extends RuntimeException {

    public KeycloakUserException(String message) {
        super(message);
    }
}
