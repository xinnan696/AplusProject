package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.domain.dto.ForgotPwdRequest;
import com.ucd.urbanflow.domain.dto.LoginRequest;
import com.ucd.urbanflow.domain.dto.ResetPwdRequest;
import com.ucd.urbanflow.domain.vo.*;
import com.ucd.urbanflow.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for handling authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest httpRequest) {
        LoginResponse loginResponse = authService.login(loginRequest, httpRequest);
        return ApiResponse.success(loginResponse);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPwdRequest forgetPwdRequest, HttpServletRequest httpRequest) {
        authService.processForgotPassword(forgetPwdRequest.getEmail(), httpRequest);
        return ApiResponse.success("Reset link sent to your email. Please check your inbox.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPwdRequest resetPwdRequest, HttpServletRequest httpRequest) {
        authService.processResetPassword(resetPwdRequest, httpRequest);
        return ApiResponse.success("Password reset successfully.");
    }

    // test data
    @GetMapping("/hash/{password}")
    public String getHashedPassword(@PathVariable String password) {
        return passwordEncoder.encode(password);
    }
}