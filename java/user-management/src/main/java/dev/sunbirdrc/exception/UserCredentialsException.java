package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserCredentialsException extends RuntimeException{
    public UserCredentialsException(String message) {
        super(message);
    }
}
