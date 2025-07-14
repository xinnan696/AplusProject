package com.ucd.urbanflow.dto.service;

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
}
