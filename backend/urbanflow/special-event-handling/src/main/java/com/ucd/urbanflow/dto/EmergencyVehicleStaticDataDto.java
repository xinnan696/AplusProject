package com.ucd.urbanflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * DTO for the static data of an emergency vehicle, used for API responses.
 * This includes only the one-time data that the frontend needs for initialization.
 */
@Data
public class EmergencyVehicleStaticDataDto {
    @JsonProperty("organization")
    private String organization;
    @JsonProperty("signalized_junctions")
    private List<String> signalizedJunctions;
}
