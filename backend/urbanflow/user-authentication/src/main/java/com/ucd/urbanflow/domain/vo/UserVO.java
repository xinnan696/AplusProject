package com.ucd.urbanflow.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Safe View Object representing a user for frontend display.
 * Maps directly to the information needed in the user list UI.
 */
@Data
@Builder
public class UserVO {
    private Long id;
    private String accountNumber; // The UserID
    private String userName;
    private String email;
    private String role;
    private boolean enabled;      // The user's status
    private List<String> managedAreas;
}