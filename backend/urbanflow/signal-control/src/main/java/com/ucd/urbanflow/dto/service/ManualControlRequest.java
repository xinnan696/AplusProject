package com.ucd.urbanflow.dto.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class ManualControlRequest {
    private String junction_id;
    private Integer duration;
    private String state;
    private String source = "manual";
}
