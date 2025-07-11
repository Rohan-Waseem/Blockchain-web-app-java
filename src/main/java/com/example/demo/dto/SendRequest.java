package com.example.demo.dto;

import lombok.Data;

@Data
public class SendRequest {
    private String senderEmail;
    private String receiverAddress;
    private double amount;

    // ✅ Add these two fields
    private String timestamp;
    private String signature;
}
