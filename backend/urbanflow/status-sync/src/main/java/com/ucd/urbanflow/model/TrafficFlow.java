package com.ucd.urbanflow.model;

import lombok.Data;

import java.util.Date;

@Data
public class TrafficFlow {
    private Integer id;
    private Date timeBucket;
    private String junctionId;
    private Integer flowRateHourly;
}
