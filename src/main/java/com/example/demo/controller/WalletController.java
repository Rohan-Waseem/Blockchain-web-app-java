package com.example.demo.controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private Firestore firestore;

    // 🔍 Get wallet by email
    @GetMapping("/{email}")
    public ResponseEntity<?> getWalletByEmail(@PathVariable String email) throws InterruptedException, ExecutionException {
        ApiFuture<QuerySnapshot> future = firestore.collection("wallets")
                .whereEqualTo("email", email)
                .get();

        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        if (docs.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Wallet not found");
        }

        // You’ll always return the first (and only) match
        Map<String, Object> wallet = docs.get(0).getData();

        return ResponseEntity.ok(wallet);
    }
}
