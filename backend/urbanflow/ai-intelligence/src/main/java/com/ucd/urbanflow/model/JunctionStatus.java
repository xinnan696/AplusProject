package com.ucd.urbanflow.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JunctionStatus {
    private String junctionid;
    @JsonProperty("edge1_vehicle_count")
    private int edge1VehicleCount;
    @JsonProperty("edge2_vehicle_count")
    private int edge2VehicleCount;
    @JsonProperty("edge1_waiting_vehicle_count")
    private int edge1WaitingVehicleCount;
    @JsonProperty("edge2_waiting_vehicle_count")
    private int edge2WaitingVehicleCount;
    @JsonProperty("edge1_light_state")
    private int edge1LightState;
    @JsonProperty("edge2_light_state")
    private int edge2LightState;
    @JsonProperty("next_switch_time")
    private int nextSwitchTime;

}
