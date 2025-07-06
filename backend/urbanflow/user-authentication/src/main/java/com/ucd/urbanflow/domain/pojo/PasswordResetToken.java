package com.ucd.urbanflow.domain.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PasswordResetToken {
    private Long id;
    private String email;
    private String token;
    private LocalDateTime expiryDate;
    private boolean used;
}