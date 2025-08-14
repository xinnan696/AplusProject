package com.ucd.urbanflow.entity;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class EmergencyVehicleEvent {
    private Long id;
    private String eventId;
    private String eventType;
    private String vehicleId;
    private String vehicleType;
    private String organization;
    private Long triggerTime;
    private String startEdgeId;
    private String endEdgeId;
    private List<String> routeEdges;
    private List<String> junctionsOnPath;
    private List<String> signalizedJunctions;
    private String eventStatus;
    private Instant createdAt;
    private Instant updatedAt;

}
