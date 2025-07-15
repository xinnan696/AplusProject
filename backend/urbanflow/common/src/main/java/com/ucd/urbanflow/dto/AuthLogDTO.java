package com.ucd.urbanflow.dto;

import lombok.Data;

@Data
public class AuthLogDTO {
    /**
     * Corresponds to the 'account_number' column.
     * The user account involved in the authentication attempt.
     */
    private String accountNumber;

    /**
     * Corresponds to the 'operation_type' column.
     * The type of authentication action, e.g., "LOGIN", "LOGOUT".
     */
    private String operationType;

    /**
     * Corresponds to the 'operation_result' column.
     * The outcome of the operation, "SUCCESS" or "FAILURE".
     */
    private String operationResult;

    /**
     * Corresponds to the 'ip_address' column.
     * The IP address of the client.
     */
    private String ipAddress;

    /**
     * Corresponds to the 'user_agent' column.
     * Information about the client's browser or device.
     */
    private String userAgent;

    /**
     * Corresponds to the 'result_message' column.
     * A message explaining the outcome, especially in case of failure.
     */
    private String resultMessage;

}