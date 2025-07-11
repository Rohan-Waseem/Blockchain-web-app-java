package com.example.demo.service;

import com.example.demo.blockchain.FirestoreBlock;
import com.example.demo.blockchain.*;
import com.example.demo.utils.WalletUtil;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
//import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.security.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
public class BlockchainService {

    private final Blockchain blockchain;
    private final Firestore firestore;

    public BlockchainService() {
        this.blockchain = Blockchain.getInstance(); // Singleton
        this.firestore = FirestoreClient.getFirestore();
        loadBlockchain(); // 🔄 Load blockchain on startup
    }

    // 🔐 Create and sign transaction from Firestore wallet
    public void addTransactionSecure(String senderEmail, String receiverAddress, double amount) throws Exception {
        DocumentReference docRef = firestore.collection("wallets").document(senderEmail);
        DocumentSnapshot snapshot = docRef.get().get();
        if (!snapshot.exists()) throw new Exception("❌ Sender wallet not found.");

        String privateKeyPem = snapshot.getString("privateKey");
        String publicKeyPem = snapshot.getString("publicKey");

        PrivateKey privateKey = WalletUtil.loadPrivateKeyFromPem(privateKeyPem);
        PublicKey publicKey = WalletUtil.loadPublicKeyFromPem(publicKeyPem);

        double balance = getBalance(senderEmail);
        if (balance < amount) throw new Exception("❌ Insufficient balance.");

       com.example.demo.blockchain.Transaction tx =
       new com.example.demo.blockchain.Transaction(senderEmail, receiverAddress, amount, publicKey, Instant.now().toString());

        tx.setSignature(null);
        tx.signTransaction(privateKey);

        blockchain.addTransaction(tx);
    }

    // 💎 Mine and persist to Firestore
    public void minePendingTransactions(String minerAddress) {
        Block newBlock = blockchain.minePendingTransactions(minerAddress);
        saveBlockToFirestore(newBlock); // ⛓️ Save mined block
        updateMinerBalance(minerAddress);
    }

    // 🔁 Save Block to Firestore
    private void saveBlockToFirestore(Block block) {
        List<FirestoreTransaction> txs = block.getTransactions().stream()
                .map(FirestoreTransaction::from)
                .collect(Collectors.toList());

        // Create a Firestore-safe block map
        DocumentReference docRef = firestore.collection("blockchain").document(block.getHash());
        docRef.set(new FirestoreBlock(block, txs));
        System.out.println("✅ Block saved to Firestore: " + block.getHash());
    }

   private void loadBlockchain() {
    try {
        ApiFuture<QuerySnapshot> future = firestore.collection("blockchain")
                .orderBy("timestamp")
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Block> loaded = new ArrayList<>();
        for (DocumentSnapshot doc : documents) {
            FirestoreBlock fsBlock = doc.toObject(FirestoreBlock.class);
            if (fsBlock != null) {
                loaded.add(fsBlock.toBlock());
            }
        }

        blockchain.setChain(loaded);
        System.out.println("✅ Blockchain loaded from Firestore: " + loaded.size() + " blocks");

        // ✅ Call balance verification
        validateAndRecalculateWalletBalances();  // <-- Add this line here

    } catch (Exception e) {
        System.err.println("❌ Error loading blockchain: " + e.getMessage());
    }
    }



    // 💰 Update Firestore balance after mining reward
   private void updateMinerBalance(String minerPublicKeyBase64) {
    try {
        if (minerPublicKeyBase64 == null || minerPublicKeyBase64.trim().isEmpty()) {
            System.err.println("❌ Miner public key is null or empty.");
            return;
        }

        // ✅ Decode + sanitize the key
        String decodedKey;
        try {
            decodedKey = java.net.URLDecoder.decode(minerPublicKeyBase64, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error decoding miner address: " + e.getMessage());
            return;
        }

        String cleanKey = decodedKey.replaceAll("\\s+", ""); // Remove whitespace
        System.out.println("🔍 Cleaned key for Firestore lookup: " + cleanKey);

        // 🔍 Query Firestore with cleaned key
        ApiFuture<QuerySnapshot> future = firestore.collection("wallets")
                .whereEqualTo("publicKey", cleanKey)
                .get();

        List<QueryDocumentSnapshot> docs = future.get().getDocuments();
        System.out.println("🔎 Found wallet docs: " + docs.size());

        if (docs.isEmpty()) {
            System.err.println("❌ No wallet found for public key: " + cleanKey);
            return;
        }

        DocumentReference docRef = docs.get(0).getReference();
        Double current = docs.get(0).getDouble("balance");
        if (current == null) current = 0.0;

        // 💰 Update balance
        docRef.update("balance", current + blockchain.getMiningReward());
        System.out.println("✅ Balance updated for miner with public key");
    } catch (Exception e) {
        e.printStackTrace();
    }
    }

   public void validateAndRecalculateWalletBalances() {
    try {
        System.out.println("🔁 Starting balance validation from blockchain...");

        // Step 1: Calculate balances from blockchain transactions
        Map<String, Double> calculatedBalances = new HashMap<>();

        for (Block block : blockchain.getChain()) {
            for (Transaction tx : block.getTransactions()) {
                String rawSender = tx.getSender();
                String rawRecipient = tx.getReceiver();
                double amount = tx.getAmount();

                // Decode sender/recipient in case they are URL-encoded
                String sender = rawSender != null && !rawSender.equals("SYSTEM")
                        ? URLDecoder.decode(rawSender, StandardCharsets.UTF_8).replaceAll("\\s+", "")
                        : "SYSTEM";

                String recipient = rawRecipient != null
                        ? URLDecoder.decode(rawRecipient, StandardCharsets.UTF_8).replaceAll("\\s+", "")
                        : null;

                if (!sender.equals("SYSTEM")) {
                    calculatedBalances.put(sender, calculatedBalances.getOrDefault(sender, 0.0) - amount);
                }

                if (recipient != null) {
                    calculatedBalances.put(recipient, calculatedBalances.getOrDefault(recipient, 0.0) + amount);
                }
            }
        }

        // Step 2: Compare calculated balances with Firestore balances
        for (Map.Entry<String, Double> entry : calculatedBalances.entrySet()) {
            String publicKey = entry.getKey();
            double calculated = entry.getValue();

            ApiFuture<QuerySnapshot> query = firestore.collection("wallets")
                    .whereEqualTo("publicKey", publicKey)
                    .get();
            List<QueryDocumentSnapshot> docs = query.get().getDocuments();

            if (docs.isEmpty()) {
                System.err.println("❌ Wallet with publicKey not found: " + publicKey);
                continue;
            }

            DocumentSnapshot walletDoc = docs.get(0);
            Double stored = walletDoc.getDouble("balance");
            if (stored == null) stored = 0.0;

            if (Math.abs(stored - calculated) > 0.01) {
                System.err.println("⚠️ Mismatch for publicKey: " + publicKey);
                System.err.println("→ Calculated: " + calculated + ", Stored: " + stored);

                // Optional: Auto-fix
                // walletDoc.getReference().update("balance", calculated);
            } else {
                System.out.println("✅ Balance valid for " + publicKey);
            }
        }

        System.out.println("🎉 Balance validation complete.");
    } catch (Exception e) {
        e.printStackTrace();
    }
    }


   public Map<String, Double> getCalculatedBalances() {
    return blockchain.calculateBalancesFromChain();
    }


    public List<Block> getFullChain() {
        return blockchain.getChain();
    }

    public List<Transaction> getPendingTransactions() {
        return blockchain.getPendingTransactions();
    }

    public double getBalance(String address) {
        return blockchain.getBalance(address);
    }
}
