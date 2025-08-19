package com.ucd.urbanflow.domain.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class CongestedRoadCount {
    private Integer id;
    private Date timeBucket;
    private Integer congestedJunctionCount;
}
