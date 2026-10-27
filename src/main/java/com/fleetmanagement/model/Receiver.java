package com.fleetmanagement.model;

public class Receiver {
    private String receiverId;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String companyName;

    public Receiver() {}

    public Receiver(String receiverId, String name, String email, String password, String phone, String companyName) {
        this.receiverId = receiverId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.companyName = companyName;
    }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}