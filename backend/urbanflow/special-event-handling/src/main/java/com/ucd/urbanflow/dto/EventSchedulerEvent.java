package com.ucd.urbanflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EventSchedulerEvent {
    @JsonProperty("event_id")
    private String eventID;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("trigger_time")
    private Integer triggerTime;

    @JsonProperty("duration")
    private Integer duration;

    @JsonProperty("lane_ids")
    private List<String> laneIds;

    // Getters and Setters
    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(Integer triggerTime) {
        this.triggerTime = triggerTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public List<String> getLaneIds() {
        return laneIds;
    }

    public void setLaneIds(List<String> laneIds) {
        this.laneIds = laneIds;
    }
}
