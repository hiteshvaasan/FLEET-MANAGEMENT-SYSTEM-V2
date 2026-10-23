package com.fleetmanagement.model;

public class ReceiverLoginRequest {
    private String receiverId;
    private String password;

    // Getters and Setters
    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}