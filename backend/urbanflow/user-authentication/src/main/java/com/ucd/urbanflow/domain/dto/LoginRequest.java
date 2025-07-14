package com.ucd.urbanflow.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for login requests. The 'accountNumber' field maps to the user's Account Number.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "AccountNumber cannot be blanked")
    private String accountNumber;

    @NotBlank(message = "Password cannot be blanked")
    private String password;
}