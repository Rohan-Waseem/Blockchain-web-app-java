package com.example.demo.blockchain;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class FirestoreTransaction {
    private String sender;
    private String receiver;
    private double amount;
    private String signature;
    private String senderPublicKeyBase64; // Firestore can't store PublicKey directly
    private String timestamp;

    // Required no-arg constructor for Firestore
    public FirestoreTransaction() {}

    // Full constructor
    public FirestoreTransaction(String sender, String receiver, double amount,
                                String signature, String senderPublicKeyBase64, String timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.signature = signature;
        this.senderPublicKeyBase64 = senderPublicKeyBase64;
        this.timestamp = timestamp;
    }

    // Getters and Setters (required for Firestore deserialization)
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSenderPublicKeyBase64() {
        return senderPublicKeyBase64;
    }

    public void setSenderPublicKeyBase64(String senderPublicKeyBase64) {
        this.senderPublicKeyBase64 = senderPublicKeyBase64;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Converts this FirestoreTransaction back to a Transaction object,
     * including converting the Base64 string to PublicKey
     */
    public Transaction toTransaction() throws Exception {
    PublicKey key = null;

    if (senderPublicKeyBase64 != null) {
        byte[] keyBytes = Base64.getDecoder().decode(senderPublicKeyBase64);
        key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    Transaction tx = new Transaction(sender, receiver, amount, key, timestamp);
    tx.setSignature(signature);
    return tx;
    }


    /**
     * Static method to convert a Transaction object to Firestore-compatible version
     */
    public static FirestoreTransaction from(Transaction tx) {
    if (tx.getSenderPublicKey() == null) {
        // Handle reward or system transactions
        return new FirestoreTransaction(
                tx.getSender(),
                tx.getReceiver(),
                tx.getAmount(),
                tx.getSignature(),
                null, // no public key
                tx.getTimestamp()
        );
    }

    String keyBase64 = Base64.getEncoder().encodeToString(tx.getSenderPublicKey().getEncoded());
    return new FirestoreTransaction(
            tx.getSender(),
            tx.getReceiver(),
            tx.getAmount(),
            tx.getSignature(),
            keyBase64,
            tx.getTimestamp()
    );
    }

    }
