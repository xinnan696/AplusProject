package com.ucd.urbanflow.domain.pojo;

import lombok.Data;
import java.time.LocalDateTime;


@Data
public class UserAreaMapping {
    
    private Long id;
    

    private Long userId;

    private String areaName;
    

    private boolean enabled;
    

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    

    private Long createdBy;
}