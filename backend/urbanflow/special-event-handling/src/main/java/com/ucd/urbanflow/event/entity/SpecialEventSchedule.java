package com.ucd.urbanflow.event.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Special Event Schedule Entity
 * Corresponds to database table: special_event_schedule
 */
@Data
public class SpecialEventSchedule {
    
    /**
     * Primary key ID
     */
    private Long id;
    
    /**
     * Unique event identifier
     */
    private String eventId;
    
    /**
     * Event type (e.g., lane_closure, vehicle_stop)
     */
    private String eventType;
    
    /**
     * Trigger time (simulation seconds)
     */
    private Integer triggerTime;
    
    /**
     * Duration (seconds)
     */
    private Integer duration;
    
    /**
     * Lane ID list (JSON format string)
     */
    private String laneIds;
    
    /**
     * Event status: pending, triggered, finished, failed
     */
    private String eventStatus;
    
    /**
     * Created timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * Updated timestamp
     */
    private LocalDateTime updatedAt;
}