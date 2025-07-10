package com.ucd.urbanflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails asynchronously.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    // @Async MODIFIED: This annotation is removed to make the call synchronous.
    public void sendPasswordResetEmail(String to, String token) throws MailException {

        String resetUrl = frontendBaseUrl + "/reset?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Password Reset Request");
        String emailBody = "Hello,\n\n"
                + "You have requested to reset your password. Please click the link below to proceed:\n\n"
                + resetUrl + "\n\n"
                + "If you did not request a password reset, please ignore this email.\n\n"
                + "Thank you,\nThe UrbanFlow System Team";
        message.setText(emailBody);
        mailSender.send(message);
        log.info("Password reset email sent to {}", to);
    }
}