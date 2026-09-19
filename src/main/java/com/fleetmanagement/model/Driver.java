package com.fleetmanagement.model;

import java.time.LocalDate;

public class Driver {
    private String driverId;
    private String driverName;
    private String phone;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private String status;
    private String licensePhotoPath;

    public Driver() {}

    public Driver(String driverId, String driverName, String phone, String licenseNumber, LocalDate licenseExpiry, String status, String licensePhotoPath) {
        this.driverId = driverId;
        this.driverName = driverName;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        this.licenseExpiry = licenseExpiry;
        this.status = status;
        this.licensePhotoPath = licensePhotoPath;
    }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public LocalDate getLicenseExpiry() { return licenseExpiry; }
    public void setLicenseExpiry(LocalDate licenseExpiry) { this.licenseExpiry = licenseExpiry; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLicensePhotoPath() { return licensePhotoPath; }
    public void setLicensePhotoPath(String licensePhotoPath) { this.licensePhotoPath = licensePhotoPath; }
}