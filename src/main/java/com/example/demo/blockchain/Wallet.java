package com.example.demo.blockchain;

import com.example.demo.utils.BlockChainUtils;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Wallet {

    private PrivateKey privateKey;
    private PublicKey publicKey;

    // 🔹 Default constructor that auto-generates key pair
    public Wallet() {
        try {
            KeyPair keyPair = generateKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
        } catch (Exception e) {
            throw new RuntimeException("Error generating key pair", e);
        }
    }

    // 🔹 Constructor using existing key pair
    public Wallet(KeyPair keyPair) {
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    // 🔹 Static method to generate a key pair
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getPublicKeyEncoded() {
    return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String getPrivateKeyEncoded() {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }


    // 🔹 Converts the public key to a string (address)
    public String getAddress() {
        return BlockChainUtils.getStringFromKey(publicKey);
    }
    
    public static PublicKey getPublicKeyFromString(String keyStr) throws Exception {
    byte[] keyBytes = Base64.getDecoder().decode(keyStr);
    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePublic(spec);
    }
    
    public static boolean verifySignature(PublicKey publicKey, String data, String signature) {
    try {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(data.getBytes(StandardCharsets.UTF_8)); // 👈 Fix encoding here
        return verifier.verify(Base64.getDecoder().decode(signature));
    } catch (Exception e) {
        System.out.println("❌ Signature verification error: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
    }



}
