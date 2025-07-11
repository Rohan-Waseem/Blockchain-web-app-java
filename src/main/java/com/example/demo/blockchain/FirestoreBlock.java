package com.example.demo.blockchain;

import java.util.List;

public class FirestoreBlock {
    private int index;
    private long timestamp;
    private List<FirestoreTransaction> transactions;
    private String previousHash;
    private String hash;
    private int nonce;

    public FirestoreBlock() {}

    public FirestoreBlock(Block block, List<FirestoreTransaction> txs) {
        this.index = block.getIndex();
        this.timestamp = block.getTimestamp();
        this.previousHash = block.getPreviousHash();
        this.hash = block.getHash();
        this.nonce = block.getNonce();
        this.transactions = txs;
    }

    public Block toBlock() throws Exception {
        List<Transaction> realTxs = transactions.stream()
                .map(tx -> {
                    try {
                        return tx.toTransaction();
                    } catch (Exception e) {
                        throw new RuntimeException("❌ Failed to convert FirestoreTransaction", e);
                    }
                })
                .toList();
        return new Block(index, timestamp, realTxs, previousHash);
    }

    // Getters and setters...
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<FirestoreTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<FirestoreTransaction> transactions) { this.transactions = transactions; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public int getNonce() { return nonce; }
    public void setNonce(int nonce) { this.nonce = nonce; }
}
