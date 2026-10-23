package com.fleetmanagement.model;

public class Checkpoint {
    private int sequence;
    private String tripId;
    private String location;
    private String expectedTime;
    private String actualTime;
    private String status;

    public Checkpoint(int sequence, String tripId, String location, String expectedTime, String actualTime, String status) {
        this.sequence = sequence;
        this.tripId = tripId;
        this.location = location;
        this.expectedTime = expectedTime;
        this.actualTime = (actualTime == null) ? "Not Reached" : actualTime;
        this.status = status;
    }

    // JavaFX requires Getters to populate the table columns
    public int getSequence() { return sequence; }
    public String getTripId() { return tripId; }
    public String getLocation() { return location; }
    public String getExpectedTime() { return expectedTime; }
    public String getActualTime() { return actualTime; }
    public String getStatus() { return status; }
}