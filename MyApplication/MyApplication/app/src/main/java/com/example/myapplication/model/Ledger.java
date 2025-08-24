package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class Ledger {
    @SerializedName("id")
    private int id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("amount")
    private int amount;
    
    @SerializedName("club")
    private int club;
    
    @SerializedName("account")
    private Object account; // null이므로 Object로 처리
    
    @SerializedName("admin")
    private Object admin; // null이므로 Object로 처리
    
    // 기본 생성자
    public Ledger() {}
    
    // Getters
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public int getClub() {
        return club;
    }
    
    public Object getAccount() {
        return account;
    }
    
    public Object getAdmin() {
        return admin;
    }
    
    // Setters
    public void setId(int id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    public void setClub(int club) {
        this.club = club;
    }
    
    public void setAccount(Object account) {
        this.account = account;
    }
    
    public void setAdmin(Object admin) {
        this.admin = admin;
    }
}