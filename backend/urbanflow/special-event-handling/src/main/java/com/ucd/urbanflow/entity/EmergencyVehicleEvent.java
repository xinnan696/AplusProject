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
    // MyBatis通常能自动处理JSON类型，如果不行则需要自定义TypeHandler
    private List<String> routeEdges;
    private List<String> signalizedJunctions;
    private String eventStatus;
    private Instant createdAt;
    private Instant updatedAt;

//    public String getEventId() {
//        return eventId;
//    }
//
//    public String getVehicleId() {
//        return vehicleId;
//    }
//
//
//    public String getOrganization() {
//        return organization;
//    }
//
//    public List<String> getRouteEdges() {
//        return routeEdges;
//    }
//
//    public List<String> getSignalizedJunctions() {
//        return signalizedJunctions;
//    }
//
//    public String getEventType() {
//        return eventType;
//    }
//
//    public String getVehicleType() {
//        return vehicleType;
//    }
}
