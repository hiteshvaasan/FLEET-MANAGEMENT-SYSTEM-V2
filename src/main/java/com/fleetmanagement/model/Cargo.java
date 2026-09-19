package com.fleetmanagement.model;

public class Cargo {
    private String cargoId; // CHANGED FROM int TO String
    private String tripId;
    private String goodsType;
    private int quantity;
    private double weightKg;
    private String description;

    public Cargo() {}

    public Cargo(String cargoId, String tripId, String goodsType, int quantity, double weightKg, String description) {
        this.cargoId = cargoId;
        this.tripId = tripId;
        this.goodsType = goodsType;
        this.quantity = quantity;
        this.weightKg = weightKg;
        this.description = description;
    }

    // Getters and Setters
    public String getCargoId() { return cargoId; }
    public void setCargoId(String cargoId) { this.cargoId = cargoId; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getGoodsType() { return goodsType; }
    public void setGoodsType(String goodsType) { this.goodsType = goodsType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}