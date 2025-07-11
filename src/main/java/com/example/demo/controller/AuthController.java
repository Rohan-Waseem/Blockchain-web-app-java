package com.example.demo.controller;

import com.example.demo.blockchain.Wallet;
import com.google.auth.Credentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/verify")
    public ResponseEntity<String> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {System.out.println("🔐 /auth/verify endpoint hit.");

            // 🔍 Extract and verify ID token
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            Firestore db = FirestoreClient.getFirestore();
            DocumentReference docRef = db.collection("wallets").document(email);
            DocumentSnapshot existing = docRef.get().get();

            if (existing.exists()) {
                return ResponseEntity.ok("✅ Wallet already exists for UID: " + uid);
            }

            // 🧠 Generate new blockchain wallet
            Wallet wallet = new Wallet();

            Map<String, Object> walletData = new HashMap<>();
            walletData.put("email", email);
            walletData.put("publicKey", wallet.getPublicKeyEncoded());
            walletData.put("privateKey", wallet.getPrivateKeyEncoded());
            walletData.put("balance", 0.0);

            // 🗂 Save wallet to Firestore
            docRef.set(walletData).addListener(() -> {
            System.out.println("✅ Wallet stored in Firestore for UID: " + uid);
        }, Runnable::run);

            return ResponseEntity.ok("✅ Token verified and wallet created for UID: " + uid);
        } catch (Exception e) {
            e.printStackTrace();  // Optional: Log for debugging
            return ResponseEntity.status(401).body("❌ Invalid token or error: " + e.getMessage());
        }
    }
}
