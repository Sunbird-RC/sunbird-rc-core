package dev.sunbirdrc.registry.service.mask;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class HashEmitStrategy implements IEmitStrategy {
    @Override
    public String updateValue(String value) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            messageDigest.update(salt);
            byte[] hashedValue = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedValue)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {}
        return "";
    }
}
