package com.ucd.urbanflow.model;

import lombok.Data;

import java.util.Date;

@Data
public class CongestedRoadCount {
    private Integer id;
    private Date timeBucket;
    private Integer congestedJunctionCount;
}
