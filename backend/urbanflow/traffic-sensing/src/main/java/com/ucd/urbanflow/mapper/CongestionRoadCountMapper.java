package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.CongestedRoadCount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CongestionRoadCountMapper {
    /**
     * Inserts a new traffic flow record into the dashboard table.
     * @param congestedRoadCount The data to insert.
     */
    void insert(CongestedRoadCount congestedRoadCount);
}
