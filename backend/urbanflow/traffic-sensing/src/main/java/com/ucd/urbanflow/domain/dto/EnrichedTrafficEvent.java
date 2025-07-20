package com.ucd.urbanflow.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * An enriched event object that combines data from multiple Redis sources.
 * This is the object published by the Acquisition layer.
 */
@Data
@Builder
public class EnrichedTrafficEvent {
    private String edgeId;
    private String junctionId;
    private String junctionName;
    private long simulationStep;
    private int vehicleCount;
    private double waitTime;
    private boolean congested;
    private int waitingVehicleCount;
    private float occupancy;

}