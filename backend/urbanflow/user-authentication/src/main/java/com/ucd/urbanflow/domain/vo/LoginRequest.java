package com.ucd.urbanflow.domain.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * [cite_start]DTO for login requests. [cite: 2]
 */
@Data
public class LoginRequest {

    @NotBlank(message = "UseID cannot be blanked")
    private String userId; // Can be username or email

    @NotBlank(message = "Password cannot be blanked")
    private String password;
}