package com.ucd.urbanflow.domain.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

/**
 * Data Transfer Object representing the JSON structure from Redis for a TLS (Traffic Light System).
 * This maps to the `sumo:tls:<tlsID>` keys.
 */
@Data
public class RedisTlsData {

    @SerializedName("tlsID")
    private String tlsId;

    @SerializedName("junction_id")
    private String junctionId;

    @SerializedName("junction_name")
    private String junctionName;

    private Double timestamp;

    /**
     * A complex list defining traffic flows through the junction.
     * Format: [[incoming_lane, outgoing_lane, internal_lane], ...]
     */
    private List<List<String>> connection;
}