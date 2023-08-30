package dev.sunbirdrc.utils;

import dev.sunbirdrc.config.PropertiesValueMapper;
import dev.sunbirdrc.exception.CipherEncoderException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;


@Component
public class CipherEncoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(CipherEncoder.class);
    @Autowired
    private PropertiesValueMapper valueMapper;

    private static byte[] IV = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static IvParameterSpec IV_SPEC = new IvParameterSpec(IV);

    /**
     * @param text
     * @return
     */
    public String encodeText(String text) {
        if (StringUtils.hasText(text)) {
            byte[] keyBytes = valueMapper.getCustomUserCredentialSecretKey().getBytes();
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            try {
                Cipher cipher = Cipher.getInstance(valueMapper.getCustomUserCipherProviderAlgorithm());

                cipher.init(Cipher.ENCRYPT_MODE, getKey(), IV_SPEC);
                byte[] cipherText = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

                return Base64.encodeBase64URLSafeString(cipherText);

            } catch (Exception e) {
                throw new CipherEncoderException("Invalid input while encoding");
            }
        } else {
            throw new CipherEncoderException("Invalid input while encoding");
        }
    }

    /**
     * @param text
     * @return
     */
    public String decodeText(String text) {
        if (StringUtils.hasText(text)) {
            byte[] keyBytes = valueMapper.getCustomUserCredentialSecretKey().getBytes();
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            try {
                Cipher cipher = Cipher.getInstance(valueMapper.getCustomUserCipherProviderAlgorithm());

                cipher.init(Cipher.DECRYPT_MODE, getKey(), IV_SPEC);

                String decodeStr = URLDecoder.decode(text, StandardCharsets.UTF_8);
                byte[] base64decodedToken = Base64.decodeBase64(decodeStr.getBytes(StandardCharsets.UTF_8));
                byte[] decryptedText = cipher.doFinal(base64decodedToken);

                return new String(decryptedText);

            } catch (Exception e) {
                throw new CipherEncoderException("Invalid input while encoding");
            }
        } else {
            throw new CipherEncoderException("Invalid input while encoding");
        }
    }

    private Key getKey() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(UserConstant.SECRET_KEY_FACTORY_ALGO);

        KeySpec spec = new PBEKeySpec(valueMapper.getCustomUserCredentialSecretKey().toCharArray(),
                UserConstant.CUSTOM_USER_KEY_SALT.getBytes(), 65536, 128);

        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        return secret;
    }
}
