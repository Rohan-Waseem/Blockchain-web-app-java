package com.example.demo.controller;

import com.example.demo.blockchain.Block;
import com.example.demo.service.BlockchainService;
import com.example.demo.blockchain.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private BlockchainService blockchainService;

    @GetMapping("/summary")
public Map<String, Object> getSummary() {
    List<Block> chain = blockchainService.getFullChain();

    int totalBlocks = chain.size();
    int totalTransactions = chain.stream()
            .mapToInt(block -> block.getTransactions().size())
            .sum();

    Map<String, Double> balances = blockchainService.getCalculatedBalances();

    // 💰 Richest wallet
    String richestWallet = "N/A";
    double highestBalance = 0;
    for (Map.Entry<String, Double> entry : balances.entrySet()) {
        if (entry.getValue() > highestBalance) {
            highestBalance = entry.getValue();
            richestWallet = entry.getKey();
        }
    }

    // 🥧 Top 5 wallets
    Map<String, Double> sortedTop = balances.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(5)
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
            ));

    // 📊 Transactions per day (as before)
    Map<String, Integer> transactionsPerDay = new TreeMap<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    for (Block block : chain) {
        List<Transaction> txs = block.getTransactions();
        if (txs == null || txs.isEmpty()) continue;

        Instant instant = Instant.ofEpochMilli(block.getTimestamp());
        String date = formatter.format(LocalDate.ofInstant(instant, ZoneId.systemDefault()));
        transactionsPerDay.put(date, transactionsPerDay.getOrDefault(date, 0) + txs.size());
    }

    // ✅ Build response
    Map<String, Object> response = new HashMap<>();
    response.put("totalBlocks", totalBlocks);
    response.put("totalTransactions", totalTransactions);
    response.put("richestWallet", richestWallet);
    response.put("highestBalance", highestBalance);
    response.put("transactionsPerDay", transactionsPerDay);
    response.put("topWallets", sortedTop);  // 👈 This is new

    return response;
}

}
