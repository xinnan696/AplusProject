package com.ucd.urbanflow.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for updating an existing user's profile.
 */
@Data
public class UpdateUserRequest {
    @NotBlank(message = "Name cannot be blank")
    private String userName;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Please provide a valid email address")
    private String email;

    private String department;
    private String phoneNumber;

    @NotBlank(message = "Role cannot be blank")
    private String role;

    private boolean enabled;
}