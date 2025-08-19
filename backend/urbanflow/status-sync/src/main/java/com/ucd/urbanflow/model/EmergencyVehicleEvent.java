package com.ucd.urbanflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class EmergencyVehicleEvent {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("vehicle_id")
    private String vehicleId;

    @JsonProperty("vehicle_type")
    private String vehicleType;

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("trigger_time")
    private Long triggerTime;

    @JsonProperty("start_edge_id")
    private String startEdgeId;

    @JsonProperty("end_edge_id")
    private String endEdgeId;

    @JsonProperty("route_edges")
    private List<String> routeEdges;

    @JsonProperty("signalized_junctions")
    private List<String> signalizedJunctions;

    @JsonProperty("event_status")
    private String eventStatus;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public Long getTriggerTime() { return triggerTime; }
    public void setTriggerTime(Long triggerTime) { this.triggerTime = triggerTime; }

    public String getStartEdgeId() { return startEdgeId; }
    public void setStartEdgeId(String startEdgeId) { this.startEdgeId = startEdgeId; }

    public String getEndEdgeId() { return endEdgeId; }
    public void setEndEdgeId(String endEdgeId) { this.endEdgeId = endEdgeId; }

    public List<String> getRouteEdges() { return routeEdges; }
    public void setRouteEdges(List<String> routeEdges) { this.routeEdges = routeEdges; }

    public List<String> getSignalizedJunctions() { return signalizedJunctions; }
    public void setSignalizedJunctions(List<String> signalizedJunctions) { this.signalizedJunctions = signalizedJunctions; }

    public String getEventStatus() { return eventStatus; }
    public void setEventStatus(String eventStatus) { this.eventStatus = eventStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
