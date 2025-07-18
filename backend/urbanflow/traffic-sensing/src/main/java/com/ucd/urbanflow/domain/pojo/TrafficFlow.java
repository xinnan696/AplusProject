package com.ucd.urbanflow.domain.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class TrafficFlow {
    private Integer id;
    private Date timeBucket;
    private String junctionId;
    private Integer flowRateHourly;
}
