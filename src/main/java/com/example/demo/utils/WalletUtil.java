package com.example.demo.utils;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.*;
import java.util.Base64;

public class WalletUtil {

    public static PrivateKey loadPrivateKeyFromPem(String pem) throws Exception {
        // Clean up PEM header/footer and whitespace
        String privateKeyPEM = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Use "EC" if you're using ECC
        return keyFactory.generatePrivate(keySpec);
    }

    public static PublicKey loadPublicKeyFromPem(String pem) throws Exception {
        // Clean up PEM header/footer and whitespace
        String publicKeyPEM = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Use "EC" if you're using ECC
        return keyFactory.generatePublic(keySpec);
    }
}
