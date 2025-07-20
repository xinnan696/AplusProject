package com.ucd.urbanflow.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JunctionStatus {
    private String junctionid;
    private int edge1VehicleCount;
    private int edge2VehicleCount;
    private int edge1WaitingVehicleCount;
    private int edge2WaitingVehicleCount;
    private int edge1LightState;
    private int edge2LightState;
    private int nextSwitchTime;

}
