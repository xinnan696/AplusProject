package com.ucd.urbanflow.dto;

import lombok.Data;

@Data
public class UserPermissionLogDTO {
    /**
     * Corresponds to the 'account_number' column.
     * The admin account that initiated the permission change.
     */
    private String accountNumber;

    /**
     * Corresponds to the 'target_account' column.
     * The user account whose permissions were changed.
     */
    private String targetAccount;

    /**
     * Corresponds to the 'operation_type' column.
     * The type of action, such as "CREATE", "UPDATE", or "DELETE".
     */
    private String operationType;

    /**
     * Corresponds to the 'operation_result' column.
     * The outcome of the operation, "SUCCESS" or "FAILURE".
     */
    private String operationResult;

    /**
     * Corresponds to the 'result_message' column.
     * An optional message describing the result.
     */
    private String resultMessage;

    /**
     * Corresponds to the 'operated_fields' column.
     * A description of what was changed (e.g., a JSON string).
     */
    private String operatedFields;
}