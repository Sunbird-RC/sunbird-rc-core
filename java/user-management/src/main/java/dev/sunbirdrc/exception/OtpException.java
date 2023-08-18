package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class OtpException extends RuntimeException {

    public OtpException(String message) {
        super(message);
    }
}
