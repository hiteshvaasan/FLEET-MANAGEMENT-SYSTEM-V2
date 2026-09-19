package com.fleetmanagement.model;

import java.time.LocalDate;

public class Maintenance {
    private int maintenanceId;
    private String vehicleId;
    private LocalDate maintenanceDate;
    private String maintenanceType;
    private String description;
    private double cost;
    private LocalDate nextServiceDate;
    private String status;

    public Maintenance() {}

    public Maintenance(int maintenanceId, String vehicleId, LocalDate maintenanceDate, String maintenanceType,
                       String description, double cost, LocalDate nextServiceDate, String status) {
        this.maintenanceId = maintenanceId;
        this.vehicleId = vehicleId;
        this.maintenanceDate = maintenanceDate;
        this.maintenanceType = maintenanceType;
        this.description = description;
        this.cost = cost;
        this.nextServiceDate = nextServiceDate;
        this.status = status;
    }

    // Formats integer 1 into "M001" for the TableView and TextFields
    public String getFormattedId() {
        return String.format("M%03d", maintenanceId);
    }

    public int getMaintenanceId() { return maintenanceId; }
    public void setMaintenanceId(int maintenanceId) { this.maintenanceId = maintenanceId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public LocalDate getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(LocalDate maintenanceDate) { this.maintenanceDate = maintenanceDate; }

    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public LocalDate getNextServiceDate() { return nextServiceDate; }
    public void setNextServiceDate(LocalDate nextServiceDate) { this.nextServiceDate = nextServiceDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}