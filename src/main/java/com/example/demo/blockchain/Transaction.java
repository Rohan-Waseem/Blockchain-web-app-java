package com.example.demo.blockchain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.security.*;
import java.util.Base64;

public class Transaction {
    private String sender;         // sender public key string
    private String receiver;       // receiver public key string
    private double amount;
    private String signature;      // Base64 encoded signature
     @JsonIgnore
    private PublicKey senderPublicKey;
    private String timestamp;

    // Updated constructor including signature
    public Transaction(String sender, String receiver, double amount, PublicKey senderPublicKey, String timestamp, String signature) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.senderPublicKey = senderPublicKey;
        this.timestamp = timestamp;
        this.signature = signature;
    }
    // Optional constructor without signature (for backward compatibility)
    public Transaction(String sender, String receiver, double amount, PublicKey senderPublicKey, String timestamp) {
        this(sender, receiver, amount, senderPublicKey, timestamp, null);
    }


    // Data to be signed
    public String getData() {
        return sender + receiver + amount;
    }

    // Sign the transaction using sender's private key
    public void signTransaction(PrivateKey privateKey) throws Exception {
        String data = getData();
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(data.getBytes());
        byte[] signatureBytes = signer.sign();
        this.signature = Base64.getEncoder().encodeToString(signatureBytes);
    }

    // Verify the transaction using sender's public key
    public boolean verifySignature(PublicKey publicKey) {
    try {
        String message = sender + "-" + receiver + "-" + String.format("%.1f", amount) + "-" + timestamp;
        return Wallet.verifySignature(publicKey, message, signature);
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

    // Convenience method to validate signature with a stored public key (if available)
    public boolean isSignatureValid() {
    if ("System".equals(sender)) return true; // Skip signature check for rewards
    try {
        return verifySignature(senderPublicKey);
    } catch (Exception e) {
        return false;
    }
}


    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public double getAmount() {
        return amount;
    }
    public void setSignature(String signature) {
        this.signature = signature;
    }
    public PublicKey getSenderPublicKey() {
    return senderPublicKey;
    }
    public String getTimestamp() {
    return this.timestamp;
}

    public String getSignature() {
        return this.signature;
    }
    @Override
    public String toString() {
        return "Transaction{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", amount=" + amount +
                ", signature='" + signature + '\'' +
                '}';
    }
}
