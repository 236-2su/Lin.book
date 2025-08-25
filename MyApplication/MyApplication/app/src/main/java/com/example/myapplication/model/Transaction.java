package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class Transaction {
    @SerializedName("id")
    private int id;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    @SerializedName("date")
    private String date;
    
    @SerializedName("amount")
    private long amount; // int -> long
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("payment_method")
    private String paymentMethod;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("ledger_id")
    private int ledgerId;
    
    @SerializedName("club_pk")
    private int clubPk;
    
    @SerializedName("vendor")
    private String vendor;
    
    @SerializedName("ledger")
    private int ledger;
    
    @SerializedName("receipt")
    private String receipt;
    
    // Getters
    public int getId() {
        return id;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public String getDate() {
        return date;
    }
    
    public long getAmount() { // int -> long
        return amount;
    }
    
    public String getType() {
        return type;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(int ledgerId) {
        this.ledgerId = ledgerId;
    }

    public int getClubPk() {
        return clubPk;
    }

    public void setClubPk(int clubPk) {
        this.clubPk = clubPk;
    }

    public String getVendor() {
        return vendor;
    }
    
    public int getLedger() {
        return ledger;
    }
    
    public String getReceipt() {
        return receipt;
    }
    
    // Setters
    public void setId(int id) {
        this.id = id;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public void setAmount(long amount) { // int -> long
        this.amount = amount;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
    
    public void setLedger(int ledger) {
        this.ledger = ledger;
    }
    
    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }
}