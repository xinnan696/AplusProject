package com.ucd.urbanflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * 用于API返回的紧急车辆静态数据 DTO。
 * 只包含前端在初始化时需要的一次性数据。
 */
@Data
public class EmergencyVehicleStaticDataDto {
    @JsonProperty("organization")
    private String organization;
    @JsonProperty("signalized_junctions")
    private List<String> signalizedJunctions;
}
