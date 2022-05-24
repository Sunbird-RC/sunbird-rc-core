package dev.sunbirdrc.registry.middleware.util;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Did {
    private String method;
    private String methodIdentifier;

    public static Did parse(String didText) throws Exception {
        String[] split = didText.split(":", 3);
        if (split.length == 3) {
            return Did.builder().method(split[1]).methodIdentifier(split[2]).build();
        } else {
            throw new Exception("Invalid Did Format: " + didText);
        }
    }
}
