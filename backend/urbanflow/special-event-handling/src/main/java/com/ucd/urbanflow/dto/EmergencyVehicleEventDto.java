package com.ucd.urbanflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class EmergencyVehicleEventDto {
    @JsonProperty("event_type")
    private String eventType = "emergency_event";
    @JsonProperty("event_id")
    private String eventId;
    @JsonProperty("vehicle_id")
    private String vehicleId;
    @JsonProperty("vehicle_type")
    private String vehicleType;
    @JsonProperty("organization")
    private String organization;
    @JsonProperty("route_edges")
    private List<String> routeEdges;
    @JsonProperty("junctions_on_path")
    private List<String> junctionsOnPath;
}
