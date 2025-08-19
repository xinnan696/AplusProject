package com.ucd.urbanflow.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLogVO {
    private Long id;
    private Long userId;
    private String accountNumber;
    private String userName;
    private String action;
    private String detail;
    private String module;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
