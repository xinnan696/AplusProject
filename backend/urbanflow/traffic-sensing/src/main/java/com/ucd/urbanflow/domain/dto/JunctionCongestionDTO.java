package com.ucd.urbanflow.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JunctionCongestionDTO {
    private String junctionId;
    private String junctionName;
//    private int maxVehicleCount;
    private int congestionCount;

}