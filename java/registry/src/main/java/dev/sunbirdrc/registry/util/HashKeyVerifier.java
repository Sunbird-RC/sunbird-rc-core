package dev.sunbirdrc.registry.util;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashKeyVerifier {
    public static void main(String[] args) {
        String originalData = "sample11-1aa1-11a1-10a0-digilockerid"; // The data that was originally hashed
        String providedHash = "NzkyODIyYmNjZDkyYmY4MWI4MzM0YTBjZmMwZmJmODUyODgzYzE4ZDZkYTg3MGNlZmJmODNlYTkxY2FmZDEyMQ=="; // The hash value provided for verification

        try {
            String algorithm = "SHA-256"; // Choose the appropriate hashing algorithm

            // Create a MessageDigest instance
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            // Convert the original data to bytes and hash it
            byte[] hashedBytes = digest.digest(originalData.getBytes(StandardCharsets.UTF_8));

            // Convert the hashed bytes to a hexadecimal string
            String generatedHash = bytesToHex(hashedBytes);

            // Compare the generated hash with the provided hash
            if (providedHash.equals(generatedHash)) {
                System.out.println("Hashes match: Valid hash key");
            } else {
                System.out.println("Hashes do not match: Invalid hash key");
            }

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Unsupported hashing algorithm: " + e.getMessage());
        }
    }

    // Helper method to convert bytes to hexadecimal string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

