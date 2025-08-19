package com.ucd.urbanflow.dto;

import lombok.Data;
import java.util.List;

@Data
public class SpecialEventLogDTO {
    /**
     * Corresponds to the 'event_id' column.
     * The unique ID of the special event.
     */
    private Long eventId;

    /**
     * Corresponds to the 'event_type' column.
     * The type of event, like "BLOCK_LANE".
     */
    private String eventType;

    /**
     * Corresponds to the 'vehicle_id' column.
     * The ID of the vehicle involved in the special event, if applicable.
     */
    private String vehicleId;

    /**
     * Corresponds to the 'lane_ids' column.
     * This list will be converted to a JSON string before database insertion.
     */
    private List<String> laneIds;

    /**
     * Corresponds to the 'duration' column.
     * The duration of the event in seconds.
     */
    private Integer duration;

    /**
     * Corresponds to the 'operation_result' column.
     * The outcome, "SUCCESS" or "FAILURE".
     */
    private String operationResult;

    /**
     * Corresponds to the 'result_message' column.
     * A message detailing the result or response.
     */
    private String resultMessage;
}