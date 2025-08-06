package com.ucd.urbanflow.domain.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class CongestedDurationRanking {
    private Integer id;
    private Date timeBucket;
    private String junctionName;
    private String junctionId;
    private Float totalCongestionDurationSeconds;
}
