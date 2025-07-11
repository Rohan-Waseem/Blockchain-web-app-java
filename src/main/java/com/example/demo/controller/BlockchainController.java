package com.example.demo.controller;

import com.example.demo.blockchain.Block;
import com.example.demo.blockchain.Blockchain;
import com.example.demo.blockchain.Transaction;
import com.example.demo.blockchain.Wallet;
import com.example.demo.dto.TransactionRequest;
import com.example.demo.service.BlockchainService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.SendRequest;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final Blockchain blockchain = Blockchain.getInstance();
    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private Firestore firestore;

    // 🔗 GET full blockchain
    @GetMapping("/chain")
    public List<Block> getFullChain() {
        return blockchain.getChain();
    }

    // 📬 POST secure transaction using Firebase-authenticated sender
    @PostMapping("/send")
    public ResponseEntity<String> sendTransaction(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SendRequest body
    ) {
    try {
        // ✅ Extract and verify Firebase token
        String idToken = authHeader.replace("Bearer ", "");
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String senderEmail = decodedToken.getEmail();

        // 🔐 Fetch sender's wallet document from Firestore
        DocumentSnapshot senderDoc = getWalletByEmail(senderEmail);
        if (senderDoc == null) {
            return ResponseEntity.status(404).body("❌ Sender wallet not found");
        }

        String senderPublicKeyStr = senderDoc.getString("publicKey");
        PublicKey senderPublicKey = Wallet.getPublicKeyFromString(senderPublicKeyStr);
        double senderBalance = senderDoc.getDouble("balance");

        // 💰 Validate amount
        if (body.getAmount() <= 0 || body.getAmount() > senderBalance) {
            return ResponseEntity.status(400).body("❌ Invalid or insufficient amount");
        }

        // 🧾 Reconstruct the signed message
        String messageToVerify = senderPublicKeyStr + "-" +
                                 body.getReceiverAddress() + "-" +
                                 body.getAmount() + "-" +
                                 body.getTimestamp();
        System.out.println("🔍 Verifying message: " + messageToVerify);
        // ✅ Verify digital signature
        if (!Wallet.verifySignature(senderPublicKey, messageToVerify, body.getSignature())) {
            return ResponseEntity.status(403).body("❌ Invalid signature");
        }
        System.out.println("🔍 Verifying message: " + messageToVerify);
        System.out.println("📬 Signature: " + body.getSignature());

        // 📦 Construct the transaction object
        Transaction tx = new Transaction(
                senderPublicKeyStr,
                body.getReceiverAddress(),
                body.getAmount(),
                senderPublicKey,
                body.getTimestamp(),
                body.getSignature()
        );

        // ➕ Add to blockchain pool
        blockchain.addTransaction(tx);
        
        // 🪙 Update balances
        updateBalance(senderEmail, -body.getAmount());
        updateBalanceByAddress(body.getReceiverAddress(), body.getAmount());
        
        return ResponseEntity.ok("✅ Transaction sent successfully");
        

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body("❌ Error sending transaction: " + e.getMessage());
    }
    }

    @PostMapping("/transaction")
    public ResponseEntity<?> sendSecureTransaction(@RequestBody TransactionRequest body) {
        try {
            if (body.getAmount() <= 0 || body.getSenderEmail() == null || body.getReceiverAddress() == null)
                return ResponseEntity.badRequest().body("❌ Invalid data.");

           blockchainService.addTransactionSecure(body.getSenderEmail(), body.getReceiverAddress(), body.getAmount());

            return ResponseEntity.ok("✅ Transaction securely submitted.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ " + e.getMessage());
        }
    }

    // ⛏️ Mine pending transactions
    @PostMapping("/mine")
    public ResponseEntity<String> mineBlock(@RequestBody Map<String, String> body) {
    try {
        String minerAddress = body.get("minerAddress");
        blockchainService.minePendingTransactions(minerAddress);
        return ResponseEntity.ok("⛏️ Block mined!");
    } catch (Exception e) {
        return ResponseEntity.status(500).body("❌ Mining failed: " + e.getMessage());
    }
}


    // 💰 Get balance of wallet
        @GetMapping("/balance")
     public double getBalance(@RequestParam String address) {
         return blockchain.getBalance(address);
     }


    // 🔍 Find wallet by email
    private DocumentSnapshot getWalletByEmail(String email) throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection("wallets")
                .whereEqualTo("email", email).get();
        List<QueryDocumentSnapshot> docs = future.get().getDocuments();
        return docs.isEmpty() ? null : docs.get(0);
    }

    // 💳 Update wallet by email
    private void updateBalance(String email, double delta) throws Exception {
        DocumentSnapshot doc = getWalletByEmail(email);
        if (doc != null) {
            double current = doc.getDouble("balance");
            firestore.collection("wallets").document(doc.getId())
                    .update("balance", current + delta);
        }
    }

    // 💳 Update wallet by address
    private void updateBalanceByAddress(String address, double delta) throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection("wallets")
                .whereEqualTo("publicKey", address).get();
        List<QueryDocumentSnapshot> docs = future.get().getDocuments();
        if (!docs.isEmpty()) {
            DocumentSnapshot doc = docs.get(0);
            double current = doc.getDouble("balance");
            firestore.collection("wallets").document(doc.getId())
                    .update("balance", current + delta);
        }
    }
    @GetMapping("/pending")
    public ResponseEntity<List<Transaction>> getPendingTransactions() {
    return ResponseEntity.ok(blockchain.getPendingTransactions());
    }


}
