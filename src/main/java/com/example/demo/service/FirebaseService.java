package com.example.demo.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseService {

    private final Firestore db = FirestoreClient.getFirestore();

    public Map<String, Double> getAllWalletBalances() {
        Map<String, Double> balances = new HashMap<>();

        try {
            ApiFuture<QuerySnapshot> future = db.collection("wallets").get();
            QuerySnapshot snapshot = future.get();

            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String address = doc.getString("publicKey");
                Double balance = doc.getDouble("balance");

                if (address != null && balance != null) {
                    balances.put(address, balance);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return balances;
    }
}
