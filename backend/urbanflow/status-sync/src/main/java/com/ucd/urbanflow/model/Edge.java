package com.ucd.urbanflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Edge {

    @JsonProperty("edgeID")
    private String edgeID;

    @JsonProperty("edgeName")
    private String edgeName;

    @JsonProperty("timestamp")
    private double timestamp;

    @JsonProperty("laneNumber")
    private int laneNumber;

    @JsonProperty("speed")
    private double speed;

    @JsonProperty("vehicleCount")
    private int vehicleCount;

    @JsonProperty("vehicleIDs")
    private List<String> vehicleIDs;

    @JsonProperty("waitTime")
    private double waitTime;

    @JsonProperty("waitingVehicleCount")
    private int waitingVehicleCount;

    // Getters and Setters
    public String getEdgeID() { return edgeID; }
    public void setEdgeID(String edgeID) { this.edgeID = edgeID; }

    public String getEdgeName() { return edgeName; }
    public void setEdgeName(String edgeName) { this.edgeName = edgeName; }

    public double getTimestamp() { return timestamp; }
    public void setTimestamp(double timestamp) { this.timestamp = timestamp; }

    public int getLaneNumber() { return laneNumber; }
    public void setLaneNumber(int laneNumber) { this.laneNumber = laneNumber; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public int getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(int vehicleCount) { this.vehicleCount = vehicleCount; }

    public List<String> getVehicleIDs() { return vehicleIDs; }
    public void setVehicleIDs(List<String> vehicleIDs) { this.vehicleIDs = vehicleIDs; }

    public double getWaitTime() { return waitTime; }
    public void setWaitTime(double waitTime) { this.waitTime = waitTime; }

    public int getWaitingVehicleCount() { return waitingVehicleCount; }
    public void setWaitingVehicleCount(int waitingVehicleCount) { this.waitingVehicleCount = waitingVehicleCount; }
}
