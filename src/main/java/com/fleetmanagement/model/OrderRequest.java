package com.fleetmanagement.model;

public class OrderRequest {
    private int requestId;
    private String receiverId, receiverName, companyName, phone;
    private String pickupLocation, deliveryLocation, pickupDate, cargoType;
    private double weight;
    private String remarks, status, requestedAt, adminRemarks, reviewedAt;

    public int getRequestId() { return requestId; } public void setRequestId(int v) { requestId = v; }
    public String getReceiverId() { return receiverId; } public void setReceiverId(String v) { receiverId = v; }
    public String getReceiverName() { return receiverName; } public void setReceiverName(String v) { receiverName = v; }
    public String getCompanyName() { return companyName; } public void setCompanyName(String v) { companyName = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
    public String getPickupLocation() { return pickupLocation; } public void setPickupLocation(String v) { pickupLocation = v; }
    public String getDeliveryLocation() { return deliveryLocation; } public void setDeliveryLocation(String v) { deliveryLocation = v; }
    public String getPickupDate() { return pickupDate; } public void setPickupDate(String v) { pickupDate = v; }
    public String getCargoType() { return cargoType; } public void setCargoType(String v) { cargoType = v; }
    public double getWeight() { return weight; } public void setWeight(double v) { weight = v; }
    public String getRemarks() { return remarks; } public void setRemarks(String v) { remarks = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getRequestedAt() { return requestedAt; } public void setRequestedAt(String v) { requestedAt = v; }
    public String getAdminRemarks() { return adminRemarks; } public void setAdminRemarks(String v) { adminRemarks = v; }
    public String getReviewedAt() { return reviewedAt; } public void setReviewedAt(String v) { reviewedAt = v; }
}