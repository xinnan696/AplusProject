package com.ucd.urbanflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Junction {

    @JsonProperty("tlsID")
    private String tlsID;

    @JsonProperty("junction_id")
    private String junctionID;

    @JsonProperty("junction_name")
    private String junctionName;

    @JsonProperty("timestamp")
    private double timestamp;

    @JsonProperty("phase")
    private int phase;

    @JsonProperty("state")
    private String state;

    @JsonProperty("duration")
    private double duration;

    // Redis中字段是 "connection"，每个连接是 List<List<String>>
    @JsonProperty("connection")
    private List<List<List<String>>> connections;

    @JsonProperty("spendTime")
    private double spendTime;

    @JsonProperty("nextSwitchTime")
    private double nextSwitchTime;

    // ====== Getter & Setter ======

    public String getTlsID() { return tlsID; }
    public void setTlsID(String tlsID) { this.tlsID = tlsID; }

    public String getJunctionID() { return junctionID; }
    public void setJunctionID(String junctionID) { this.junctionID = junctionID; }

    public String getJunctionName() { return junctionName; }
    public void setJunctionName(String junctionName) { this.junctionName = junctionName; }

    public double getTimestamp() { return timestamp; }
    public void setTimestamp(double timestamp) { this.timestamp = timestamp; }

    public int getPhase() { return phase; }
    public void setPhase(int phase) { this.phase = phase; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }

    public List<List<List<String>>> getConnections() { return connections; }
    public void setConnections(List<List<List<String>>> connections) { this.connections = connections; }

    public double getSpendTime() { return spendTime; }
    public void setSpendTime(double spendTime) { this.spendTime = spendTime; }

    public double getNextSwitchTime() { return nextSwitchTime; }
    public void setNextSwitchTime(double nextSwitchTime) { this.nextSwitchTime = nextSwitchTime; }
}
