package com.ucd.urbanflow.domain.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object representing the JSON structure from Redis for an edge.
 * This maps to the `sumo:edge:<edgeID>` keys.
 */
@Data
public class RedisEdgeData {

    @SerializedName("edgeID")
    private String edgeId;

    private String edgeName;
    private Double timestamp;
    private Integer laneNumber;
    private Float speed;
    private Integer vehicleCount;
    private List<String> vehicleIDs;
    private Double waitTime;
    private List<String> waitingVehicleIDs;
    private Integer waitingVehicleCount;
    private Float occupancy;
}