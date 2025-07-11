/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.dto;

import lombok.Data;

/**
 *
 * @author HP
 */
@Data
public class TransactionRequest {
    private String senderEmail;
    private String receiverAddress;
    private double amount;
    private String signature;         // ⬅️ NEW
    private String timestamp;    
    // ⬅️ Optional but recommended
}

