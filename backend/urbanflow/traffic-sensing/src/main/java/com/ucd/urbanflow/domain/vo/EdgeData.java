package com.ucd.urbanflow.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdgeData {

    @JsonProperty("edgeID")
    private String edgeID;

    @JsonProperty("vehicleCount")
    private int vehicleCount;

    @JsonProperty("occupancy")
    private float occupancy;

}