package com.ucd.urbanflow.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * [cite_start]DTO for forgot password requests. [cite: 7]
 */
@Data
public class ForgotPwdRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Please enter a valid email address")
    private String email;
}