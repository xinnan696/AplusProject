package com.ucd.urbanflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class ManualControlRequest {
    private String junctionId;
    private Integer lightIndex;
    private Integer duration;
    private String state;
    private String source = "manual";
}
