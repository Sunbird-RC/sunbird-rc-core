package dev.sunbirdrc.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CipherEncoderException extends RuntimeException {

    public CipherEncoderException(String message) {
        super(message);
    }
}
