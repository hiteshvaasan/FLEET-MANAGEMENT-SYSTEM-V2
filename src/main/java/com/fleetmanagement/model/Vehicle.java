package com.fleetmanagement.model;

public class Vehicle {

    private String vehicleId;
    private String registrationNumber;
    private String vehicleType;
    private String model;
    private double capacityKg;
    private String fuelType;
    private String status;

    // ------------------------------------------------------------
    // Default constructor
    // ------------------------------------------------------------
    public Vehicle() {
    }

    // ------------------------------------------------------------
    // Parameterized constructor
    // ------------------------------------------------------------
    public Vehicle(String vehicleId, String registrationNumber, String vehicleType,
                   String model, double capacityKg, String fuelType, String status) {
        this.vehicleId = vehicleId;
        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
        this.model = model;
        this.capacityKg = capacityKg;
        this.fuelType = fuelType;
        this.status = status;
    }

    // ------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------
    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getCapacityKg() {
        return capacityKg;
    }

    public void setCapacityKg(double capacityKg) {
        this.capacityKg = capacityKg;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "vehicleId='" + vehicleId + '\'' +
                ", registrationNumber='" + registrationNumber + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", model='" + model + '\'' +
                ", capacityKg=" + capacityKg +
                ", fuelType='" + fuelType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}