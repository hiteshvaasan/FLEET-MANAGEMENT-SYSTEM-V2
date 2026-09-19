package com.fleetmanagement.model;

import java.time.LocalDate;

public class Toll {
    private String tollId;
    private String tripId;
    private String tollName;
    private String location;
    private double amount;
    private LocalDate tollDate;

    public Toll() {}

    public Toll(String tollId, String tripId, String tollName, String location, double amount, LocalDate tollDate) {
        this.tollId = tollId;
        this.tripId = tripId;
        this.tollName = tollName;
        this.location = location;
        this.amount = amount;
        this.tollDate = tollDate;
    }

    public String getTollId() { return tollId; }
    public void setTollId(String tollId) { this.tollId = tollId; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getTollName() { return tollName; }
    public void setTollName(String tollName) { this.tollName = tollName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getTollDate() { return tollDate; }
    public void setTollDate(LocalDate tollDate) { this.tollDate = tollDate; }
}