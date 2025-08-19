package com.ucd.urbanflow.dto;

import lombok.Data;

@Data
public class SignalControlLogDTO {
    /**
     * Corresponds to the 'account_number' column.
     * The account of the user or system performing the action.
     */
    private String accountNumber;

    /**
     * Corresponds to the 'junction_id' column.
     * The identifier for the controlled intersection.
     */
    private String junctionId;

    /**
     * Corresponds to the 'light_index' column.
     * The specific signal phase that was controlled.
     */
    private Integer lightIndex;

    /**
     * Corresponds to the 'light_state' column.
     * The state the signal was set to (e.g., "GrGr").
     */
    private String lightState;

    /**
     * Corresponds to the 'duration' column.
     * The duration of the signal state in seconds.
     */
    private int duration;

    /**
     * Corresponds to the 'operation_source' column.
     * The source of the control action, either "manual" or "ai".
     */
    private String operationSource;

    /**
     * Corresponds to the 'operation_result' column.
     * The outcome of the operation, "SUCCESS" or "FAILURE".
     */
    private String operationResult;

    /**
     * Corresponds to the 'result_message' column.
     * Any additional message, such as an error detail.
     */
    private String resultMessage;
}