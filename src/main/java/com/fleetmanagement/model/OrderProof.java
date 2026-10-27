package com.fleetmanagement.model;

public class OrderProof {
    private int reportId;
    private String tripId, driverId, driverName, vehicleId, source, destination, receiverId;
    // driver evidence
    private Integer driverQty;
    private String driverStatus, driverRemarks, driverReceiverName, reportedAt;
    // receiver evidence
    private String receiverStatus, receiverNotes, receiverPerson;
    private Integer receiverRating, receiverQty;
    // admin decision
    private String adminStatus, adminRemarks, adminReviewedAt;

    public int getReportId() { return reportId; } public void setReportId(int v) { reportId = v; }
    public String getTripId() { return tripId; } public void setTripId(String v) { tripId = v; }
    public String getDriverId() { return driverId; } public void setDriverId(String v) { driverId = v; }
    public String getDriverName() { return driverName; } public void setDriverName(String v) { driverName = v; }
    public String getVehicleId() { return vehicleId; } public void setVehicleId(String v) { vehicleId = v; }
    public String getSource() { return source; } public void setSource(String v) { source = v; }
    public String getDestination() { return destination; } public void setDestination(String v) { destination = v; }
    public String getReceiverId() { return receiverId; } public void setReceiverId(String v) { receiverId = v; }
    public Integer getDriverQty() { return driverQty; } public void setDriverQty(Integer v) { driverQty = v; }
    public String getDriverStatus() { return driverStatus; } public void setDriverStatus(String v) { driverStatus = v; }
    public String getDriverRemarks() { return driverRemarks; } public void setDriverRemarks(String v) { driverRemarks = v; }
    public String getDriverReceiverName() { return driverReceiverName; } public void setDriverReceiverName(String v) { driverReceiverName = v; }
    public String getReportedAt() { return reportedAt; } public void setReportedAt(String v) { reportedAt = v; }
    public String getReceiverStatus() { return receiverStatus; } public void setReceiverStatus(String v) { receiverStatus = v; }
    public String getReceiverNotes() { return receiverNotes; } public void setReceiverNotes(String v) { receiverNotes = v; }
    public String getReceiverPerson() { return receiverPerson; } public void setReceiverPerson(String v) { receiverPerson = v; }
    public Integer getReceiverRating() { return receiverRating; } public void setReceiverRating(Integer v) { receiverRating = v; }
    public Integer getReceiverQty() { return receiverQty; } public void setReceiverQty(Integer v) { receiverQty = v; }
    public String getAdminStatus() { return adminStatus; } public void setAdminStatus(String v) { adminStatus = v; }
    public String getAdminRemarks() { return adminRemarks; } public void setAdminRemarks(String v) { adminRemarks = v; }
    public String getAdminReviewedAt() { return adminReviewedAt; } public void setAdminReviewedAt(String v) { adminReviewedAt = v; }

    // Normalises the different status wordings used by driver and receiver
    private static String category(String s) {
        if (s == null || s.isBlank()) return null;
        String x = s.toLowerCase();
        if (x.contains("damag")) return "DAMAGED";
        if (x.contains("partial") || x.contains("missing")) return "PARTIAL";
        if (x.contains("reject")) return "REJECTED";
        if (x.contains("deliver")) return "DELIVERED";
        return x;
    }

    /** Used by the table column "matchResult" */
    public String getMatchResult() {
        String d = category(driverStatus);
        String r = category(receiverStatus);
        if (d == null && r == null) return "No Data";
        if (d == null) return "Awaiting Driver";
        if (r == null) return "Awaiting Receiver";
        boolean statusSame = d.equals(r);
        boolean qtySame = driverQty == null || receiverQty == null || driverQty.equals(receiverQty);
        return (statusSame && qtySame) ? "Matched" : "Mismatch";
    }
}