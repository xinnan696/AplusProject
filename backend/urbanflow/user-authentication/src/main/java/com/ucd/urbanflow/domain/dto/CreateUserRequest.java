package com.ucd.urbanflow.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * DTO for creating a new user from the administration panel.
 * Fields match the "Add New User" form.
 */
@Data
public class CreateUserRequest {
    @NotBlank(message = "Account Number cannot be blank")
    private String accountNumber;

    @NotBlank(message = "Name cannot be blank")
    private String userName;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String department;
    private String phoneNumber;

    @NotBlank(message = "Role cannot be blank")
    private String role; // e.g., "ROLE_USER"

    private boolean enabled;

    private List<String> managedAreas;
}