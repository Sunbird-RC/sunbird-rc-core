package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserConflictException extends RuntimeException {

    public UserConflictException(String message) {
        super(message);
    }
}
