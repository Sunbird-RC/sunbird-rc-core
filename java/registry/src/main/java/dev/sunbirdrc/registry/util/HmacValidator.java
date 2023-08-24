package dev.sunbirdrc.registry.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HmacValidator {

    public static boolean validateHmac(String xml, String hmacFromRequest, String secret) throws Exception {
        String hmac = generateHMACSHA256(xml, secret);
        String base64Hmac = base64Encode(hmac);
        return (validateHMAC(hmacFromRequest,base64Hmac));
    }

    private static String generateHMACSHA256(String data, String secretKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexHmac = new StringBuilder();
        for (byte b : rawHmac) {
            String hex = String.format("%02x", b);
            hexHmac.append(hex);
        }
        return hexHmac.toString();
    }

    private static String base64Encode(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean validateHMAC(String actualHMAC, String expectedHMAC) {
        return actualHMAC.equals(expectedHMAC);
    }
}

