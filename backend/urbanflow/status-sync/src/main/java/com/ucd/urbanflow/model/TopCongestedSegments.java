package com.ucd.urbanflow.model;

import lombok.Data;

import java.util.Date;

@Data
public class TopCongestedSegments {
    private Integer id;
    private Date timeBucket;
    private String junctionName;
    private String junctionId;
    private Integer congestionTimes;
}
