package com.fleetmanagement.model;

public class Fuel {
    private String fuelId;
    private String tripId;
    private String vehicleId;
    private String fuelType;
    private double distance;
    private double fuelUsed;
    private double fuelPrice;
    private double fuelCost;
    private double mileage;

    public Fuel() {}

    public Fuel(String fuelId, String tripId, String vehicleId, String fuelType, double distance, double fuelUsed, double fuelPrice, double fuelCost, double mileage) {
        this.fuelId = fuelId;
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.fuelType = fuelType;
        this.distance = distance;
        this.fuelUsed = fuelUsed;
        this.fuelPrice = fuelPrice;
        this.fuelCost = fuelCost;
        this.mileage = mileage;
    }

    // Getters and Setters
    public String getFuelId() { return fuelId; }
    public void setFuelId(String fuelId) { this.fuelId = fuelId; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public double getFuelUsed() { return fuelUsed; }
    public void setFuelUsed(double fuelUsed) { this.fuelUsed = fuelUsed; }

    public double getFuelPrice() { return fuelPrice; }
    public void setFuelPrice(double fuelPrice) { this.fuelPrice = fuelPrice; }

    public double getFuelCost() { return fuelCost; }
    public void setFuelCost(double fuelCost) { this.fuelCost = fuelCost; }

    public double getMileage() { return mileage; }
    public void setMileage(double mileage) { this.mileage = mileage; }
}