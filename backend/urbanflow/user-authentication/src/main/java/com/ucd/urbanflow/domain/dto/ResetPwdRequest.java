package com.ucd.urbanflow.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * [cite_start]DTO for reset password requests. [cite: 14]
 */
@Data
public class ResetPwdRequest {

    @NotBlank(message = "Token cannot be blank")
    private String token;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}