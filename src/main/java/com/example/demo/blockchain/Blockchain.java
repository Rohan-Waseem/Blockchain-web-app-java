package com.example.demo.blockchain;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
public class Blockchain {

    private static Blockchain instance;

    private List<Block> chain;
    private List<Transaction> pendingTransactions;
    private int difficulty;
    private final double miningReward = 1.0;
    private final Firestore firestore = FirestoreClient.getFirestore();
    // Singleton accessor
    public static synchronized Blockchain getInstance() {
        if (instance == null) {
            instance = new Blockchain(2); // You can adjust difficulty here
        }
        return instance;
    }

    // Private constructor for Singleton
    private Blockchain(int difficulty) {
        this.difficulty = difficulty;
        this.chain = new ArrayList<>();
        this.pendingTransactions = new ArrayList<>();
        Block genesis = new Block(0, System.currentTimeMillis(), new ArrayList<>(), "0");
        genesis.mineBlock(difficulty);
        chain.add(genesis);
    }

    public void addTransaction(Transaction tx) {
        if (tx == null || !tx.isSignatureValid()) {
            throw new IllegalArgumentException("Invalid transaction.");
        }
        pendingTransactions.add(tx);
    }

    public Block minePendingTransactions(String minerAddress) {
    // ✅ Step 1: Create and add reward transaction
    Transaction rewardTx = new Transaction(
        "System",
        minerAddress,
        1.0,
        null,
        Instant.now().toString(),
        null
    );
    pendingTransactions.add(rewardTx);

    // ✅ Step 2: Create a block with all pending transactions
    Block block = new Block(
        chain.size(),
        System.currentTimeMillis(),
        new ArrayList<>(pendingTransactions),
        getLatestBlock().getHash()
    );

    block.mineBlock(difficulty);
    chain.add(block);
    pendingTransactions.clear();  // ✅ Clear after adding block
    
    return block;
    }

    public Map<String, Double> calculateBalancesFromChain() {
        Map<String, Double> balances = new HashMap<>();

        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                if (!tx.getSender().equals("System")) {
                    balances.put(tx.getSender(), balances.getOrDefault(tx.getSender(), 0.0) - tx.getAmount());
                }
                balances.put(tx.getReceiver(), balances.getOrDefault(tx.getReceiver(), 0.0) + tx.getAmount());
            }
        }

        return balances;
    }

    

    public double getBalance(String address) {
        double balance = 0.0;
        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                if (tx.getSender().equals(address)) {
                    balance -= tx.getAmount();
                }
                if (tx.getReceiver().equals(address)) {
                    balance += tx.getAmount();
                }
            }
        }
        return balance;
    }

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.getHash().equals(current.calculateHash())) return false;
            if (!current.getPreviousHash().equals(previous.getHash())) return false;
        }
        return true;
    }

    public List<Block> getChain() {
        return chain;
    }

    public List<Transaction> getPendingTransactions() {
        return pendingTransactions;
    }
    public void setChain(List<Block> newChain) {
    if (newChain != null && !newChain.isEmpty()) {
        this.chain = newChain;
    }
    }

    public double getMiningReward() {
        return miningReward;
    }

}
