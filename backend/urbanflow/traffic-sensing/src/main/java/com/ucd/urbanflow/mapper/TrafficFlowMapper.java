package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.TrafficFlow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrafficFlowMapper {
    /**
     * Inserts a new traffic flow record into the dashboard table.
     * @param trafficFlow The data to insert.
     */
    void insert(TrafficFlow trafficFlow);
}