package com.fleetmanagement.model;

import java.time.LocalDate;

public class Trip {

    private String tripId;
    private String vehicleId;
    private String driverId;
    private String source;
    private String destination;
    private LocalDate tripDate;
    private Double startOdometer;
    private Double endOdometer;
    private String status;
    private Double totalFuelCost;
    private Double totalTollCost;
    private Double totalOtherExpenses;
    private String receiverId;
    private LocalDate expectedDeliveryDate;


    // ------------------------------------------------------------
    // Default constructor
    // ------------------------------------------------------------
    public Trip() {
    }

    // ------------------------------------------------------------
    // Parameterized constructor
    // ------------------------------------------------------------
    public Trip(String tripId, String vehicleId, String driverId, String source,
                String destination, LocalDate tripDate, Double startOdometer,
                Double endOdometer, String status, Double totalFuelCost,
                Double totalTollCost, Double totalOtherExpenses) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.source = source;
        this.destination = destination;
        this.tripDate = tripDate;
        this.startOdometer = startOdometer;
        this.endOdometer = endOdometer;
        this.status = status;
        this.totalFuelCost = totalFuelCost;
        this.totalTollCost = totalTollCost;
        this.totalOtherExpenses = totalOtherExpenses;
    }

    // ------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------
    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalDate getTripDate() {
        return tripDate;
    }

    public void setTripDate(LocalDate tripDate) {
        this.tripDate = tripDate;
    }

    public Double getStartOdometer() {
        return startOdometer;
    }

    public void setStartOdometer(Double startOdometer) {
        this.startOdometer = startOdometer;
    }

    public Double getEndOdometer() {
        return endOdometer;
    }

    public void setEndOdometer(Double endOdometer) {
        this.endOdometer = endOdometer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getTotalFuelCost() {
        return totalFuelCost;
    }

    public void setTotalFuelCost(Double totalFuelCost) {
        this.totalFuelCost = totalFuelCost;
    }

    public Double getTotalTollCost() {
        return totalTollCost;
    }

    public void setTotalTollCost(Double totalTollCost) {
        this.totalTollCost = totalTollCost;
    }

    public Double getTotalOtherExpenses() {
        return totalOtherExpenses;
    }

    public void setTotalOtherExpenses(Double totalOtherExpenses) {
        this.totalOtherExpenses = totalOtherExpenses;
    }

    // Add to constructor or use setter:
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }
    // ------------------------------------------------------------
    // Helper method: total cost of the trip
    // Treats any null cost component as 0 to avoid NPEs on
    // partially-loaded or newly-created Trip objects.
    // ------------------------------------------------------------
    public double getTotalTripCost() {
        double fuel = (totalFuelCost != null) ? totalFuelCost : 0.0;
        double toll = (totalTollCost != null) ? totalTollCost : 0.0;
        double other = (totalOtherExpenses != null) ? totalOtherExpenses : 0.0;
        return fuel + toll + other;
    }

    @Override
    public String toString() {
        return "Trip{" +
                "tripId='" + tripId + '\'' +
                ", vehicleId='" + vehicleId + '\'' +
                ", driverId='" + driverId + '\'' +
                ", source='" + source + '\'' +
                ", destination='" + destination + '\'' +
                ", tripDate=" + tripDate +
                ", startOdometer=" + startOdometer +
                ", endOdometer=" + endOdometer +
                ", status='" + status + '\'' +
                ", totalFuelCost=" + totalFuelCost +
                ", totalTollCost=" + totalTollCost +
                ", totalOtherExpenses=" + totalOtherExpenses +
                '}';
    }
}