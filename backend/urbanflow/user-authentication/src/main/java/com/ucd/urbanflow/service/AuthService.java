package com.ucd.urbanflow.service;

import com.ucd.urbanflow.domain.dto.LoginRequest;
import com.ucd.urbanflow.domain.dto.ResetPwdRequest;
import com.ucd.urbanflow.domain.pojo.PasswordResetToken;
import com.ucd.urbanflow.domain.pojo.User;
import com.ucd.urbanflow.domain.vo.LoginResponse;
import com.ucd.urbanflow.domain.vo.UserVO;
import com.ucd.urbanflow.mapper.PasswordResetTokenMapper;
import com.ucd.urbanflow.mapper.UserMapper;
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
    private final TransactionTemplate transactionTemplate; // 2. 注入 TransactionTemplate

    /**
     * Handles the user login process. This method is primarily read-only
     * and does not require a transaction wrapper.
     */
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new UsernameNotFoundException("User not found. Please check your accountNumber."));

        if (loginAttemptService.isBlocked(user.getEmail())) {
            throw new RuntimeException("Too many failed attempts. Try again in 5 mins.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(user.getEmail());
            throw new BadCredentialsException("Incorrect password. Please try again!");
        }

        loginAttemptService.loginSucceeded(user.getEmail());
        String jwt = jwtService.generateToken(user);
        UserVO userVO = mapToUserVO(user);
        return LoginResponse.builder().token(jwt).user(userVO).build();
    }

    /**
     * Processes a 'forgot password' request using programmatic transaction management.
     */
    public void processForgotPassword(String email) {
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

            } catch (Exception e) {
                log.error("Exception during forgot password transaction for email {}: {}", email, e.getMessage());
                status.setRollbackOnly();
                throw new RuntimeException("Failed to process password reset request. Please try again.", e);
            }
            return null;
        });
    }

    /**
     * Processes a 'reset password' request using programmatic transaction management.
     * This ensures that updating the password and marking the token as used
     * either both succeed or both fail.
     */
    public void processResetPassword(ResetPwdRequest request) {
        transactionTemplate.execute(status -> {
            try {
                PasswordResetToken resetToken = tokenMapper.findByToken(request.getToken())
                        .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

                if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                    throw new RuntimeException("Reset link has expired. Please request again");
                }

                String email = resetToken.getEmail();
                userMapper.updatePassword(email, passwordEncoder.encode(request.getNewPassword()));
                tokenMapper.markAsUsed(request.getToken());
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException(e);
            }
            return null;
        });
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