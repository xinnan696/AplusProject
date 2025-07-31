package com.ucd.urbanflow.service;

import com.ucd.urbanflow.client.LogServiceClient;
import com.ucd.urbanflow.domain.dto.LoginRequest;
import com.ucd.urbanflow.domain.dto.ResetPwdRequest;
import com.ucd.urbanflow.domain.pojo.PasswordResetToken;
import com.ucd.urbanflow.domain.pojo.User;
import com.ucd.urbanflow.domain.vo.ApiResponse;
import com.ucd.urbanflow.domain.vo.LoginResponse;
import com.ucd.urbanflow.domain.vo.UserVO;
import com.ucd.urbanflow.dto.AuthLogDTO;
import com.ucd.urbanflow.mapper.PasswordResetTokenMapper;
import com.ucd.urbanflow.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Main service for handling all authentication-related business logic.
 * This version uses programmatic transaction management via TransactionTemplate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordResetTokenMapper tokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;
    private final TransactionTemplate transactionTemplate;
    private final AreaManagementService areaManagementService;
    private final LogServiceClient logServiceClient;

    /**
     * Handles the user login process. This method is primarily read-only
     * and does not require a transaction wrapper.
     */
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userMapper.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> {
                    recordAuthLog(request.getAccountNumber(), null, "LOGIN", "FAILURE", "User not found", httpRequest);
                    return new UsernameNotFoundException("User not found. Please check your accountNumber.");
                });

        if (loginAttemptService.isBlocked(user.getEmail())) {
            String message = "Too many failed attempts. Try again in 5 mins.";
            recordAuthLog(user, "LOGIN", "FAILURE", message, httpRequest);
            throw new RuntimeException("Too many failed attempts. Try again in 5 mins.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getAccountNumber(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(user.getEmail());
            recordAuthLog(user, "LOGIN", "FAILURE", "Incorrect password", httpRequest);
            throw new BadCredentialsException("Incorrect password. Please try again!");
        }

        loginAttemptService.loginSucceeded(user.getEmail());

        recordAuthLog(user, "LOGIN", "SUCCESS", "User login successful", httpRequest);

        String jwt = jwtService.generateToken(user);
        UserVO userVO = mapToUserVO(user);

        List<String> managedAreas = areaManagementService.getUserManagedAreas(user.getId());
        log.info("User {} (ID: {}) managed areas: {}", user.getAccountNumber(), user.getId(), managedAreas);
        userVO.setManagedAreas(managedAreas);
        
        return LoginResponse.builder().token(jwt).user(userVO).build();
    }

    /**
     * Processes a 'forgot password' request using programmatic transaction management.
     */
    public void processForgotPassword(String email, HttpServletRequest httpRequest) {
        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("This email is not associated with any account"));

        transactionTemplate.execute(status -> {
            try {
                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = new PasswordResetToken();
                resetToken.setEmail(user.getEmail());
                resetToken.setToken(token);
                resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
                resetToken.setUsed(false);

                tokenMapper.save(resetToken);
                emailService.sendPasswordResetEmail(user.getEmail(), token);
                recordAuthLog(user, "FORGOT_PASSWORD", "SUCCESS", "Password reset token requested", httpRequest);

            } catch (Exception e) {
                log.error("Exception during forgot password transaction for email {}: {}", email, e.getMessage());
                recordAuthLog(user, "FORGOT_PASSWORD", "FAILURE", e.getMessage(), httpRequest);
                status.setRollbackOnly();
                throw new RuntimeException("Failed to process password reset request. Please try again.", e);
            }
            return null;
        });
    }

    /**
     * Processes a 'reset password' request using programmatic transaction management.
     * This ensures that updating the password and marking the token as used
     */
    public void processResetPassword(ResetPwdRequest request, HttpServletRequest httpRequest) {

        PasswordResetToken resetToken = tokenMapper.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset link has expired. Please request again");
        }
        User user = userMapper.findByEmail(resetToken.getEmail()).orElseThrow(() -> new RuntimeException("User associated with token not found."));

        transactionTemplate.execute(status -> {
            try {

                String email = resetToken.getEmail();
                userMapper.updatePassword(email, passwordEncoder.encode(request.getNewPassword()));
                tokenMapper.markAsUsed(request.getToken());

                recordAuthLog(user, "RESET_PASSWORD", "SUCCESS", "User password has been reset successfully", httpRequest);

            } catch (Exception e) {

                recordAuthLog(user, "RESET_PASSWORD", "FAILURE", e.getMessage(), httpRequest);
                status.setRollbackOnly();
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    private void recordAuthLog(User user, String operationType, String operationResult, String resultMessage, HttpServletRequest request) {
        recordAuthLog(user.getAccountNumber(), user.getUserName(), operationType, operationResult, resultMessage, request);
    }

    private void recordAuthLog(String accountNumber, String userName, String operationType, String operationResult, String resultMessage, HttpServletRequest request) {
        AuthLogDTO logDTO = new AuthLogDTO();
        logDTO.setAccountNumber(accountNumber);
//        logDTO.setUserName(userName);
        logDTO.setOperationType(operationType);
        logDTO.setOperationResult(operationResult);
        logDTO.setResultMessage(resultMessage);

        if (request != null) {
            logDTO.setIpAddress(request.getRemoteAddr());
            logDTO.setUserAgent(request.getHeader("User-Agent"));
        }

        try {
            logServiceClient.logAuth(logDTO);
        } catch (Exception e) {
            // Log locally if the logging service is down, to avoid breaking the main flow
            log.error("Failed to send authentication log to logging service. Log data: {}", logDTO, e);
        }
    }

    /**
     * 临时调试方法
     */
    public ApiResponse<Object> debugUserAreas(Long userId) {
        Object result = areaManagementService.debugUserAreas(userId);
        return ApiResponse.success(result);
    }

    private void recordAuthLog(User user, String operationType, String operationResult, String resultMessage, HttpServletRequest request) {
        recordAuthLog(user.getAccountNumber(), user.getUserName(), operationType, operationResult, resultMessage, request);
    }

    private void recordAuthLog(String accountNumber, String userName, String operationType, String operationResult, String resultMessage, HttpServletRequest request) {
        AuthLogDTO logDTO = new AuthLogDTO();
        logDTO.setAccountNumber(accountNumber);
//        logDTO.setUserName(userName);
        logDTO.setOperationType(operationType);
        logDTO.setOperationResult(operationResult);
        logDTO.setResultMessage(resultMessage);

        if (request != null) {
            logDTO.setIpAddress(request.getRemoteAddr());
            logDTO.setUserAgent(request.getHeader("User-Agent"));
        }

        try {
            logServiceClient.logAuth(logDTO);
        } catch (Exception e) {
            // Log locally if the logging service is down, to avoid breaking the main flow
            log.error("Failed to send authentication log to logging service. Log data: {}", logDTO, e);
        }
    }

    /**
     * Private helper method to map a User POJO to a UserVO.
     */
    private UserVO mapToUserVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .accountNumber(user.getAccountNumber())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .build();
    }
}