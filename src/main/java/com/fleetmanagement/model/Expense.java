package com.fleetmanagement.model;

import java.time.LocalDate;

public class Expense {
    private String expenseId; // Changed from int to String
    private String tripId;
    private String expenseType;
    private String description;
    private double amount;
    private LocalDate expenseDate;

    public Expense() {}

    public Expense(String expenseId, String tripId, String expenseType, String description, double amount, LocalDate expenseDate) {
        this.expenseId = expenseId;
        this.tripId = tripId;
        this.expenseType = expenseType;
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate;
    }

    public String getExpenseId() { return expenseId; }
    public void setExpenseId(String expenseId) { this.expenseId = expenseId; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
}