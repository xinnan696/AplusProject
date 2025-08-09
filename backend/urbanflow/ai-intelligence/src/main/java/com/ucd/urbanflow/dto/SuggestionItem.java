package com.ucd.urbanflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SuggestionItem {
    @JsonProperty("suggestion_label")
    private int suggestionLabel;

    private String junction;
    private String edge;

    @JsonProperty("target_light_from")
    private String targetLightFrom;

    @JsonProperty("target_light_to")
    private String targetLightTo;

    @JsonProperty("target_state")
    private String targetState;

    private int duration;
}
