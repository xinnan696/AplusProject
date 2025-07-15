package com.ucd.urbanflow.domain.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class TopCongestedSegments {
    private Integer id;
    private Date timeBucket;
    private String junctionName;
    private Integer congestionTimes;
}
