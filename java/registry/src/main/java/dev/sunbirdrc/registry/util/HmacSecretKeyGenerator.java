package dev.sunbirdrc.registry.util;

import java.security.SecureRandom;
import java.util.Base64;

public class HmacSecretKeyGenerator {

    public static void main(String[] args) {
        // Choose the length of the secret key in bytes
        int keyLengthBytes = 32; // Adjust the length as needed
        // y+a/tzfzB3fJrpmT3OlwimIIE31Ns+N6rHOI2mCGoQQ=
        // Generate a secure random key
        byte[] secretKey = generateSecureRandomKey(keyLengthBytes);

        // Convert the byte array to a Base64-encoded string
        String base64SecretKey = Base64.getEncoder().encodeToString(secretKey);

        System.out.println("Generated HMAC Secret Key: " + base64SecretKey);
    }

    public static byte[] generateSecureRandomKey(int keyLengthBytes) {
        byte[] key = new byte[keyLengthBytes];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        return key;
    }
}
