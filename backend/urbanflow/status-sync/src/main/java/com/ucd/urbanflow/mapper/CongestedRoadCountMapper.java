package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.model.CongestedRoadCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface CongestedRoadCountMapper {
    List<CongestedRoadCount> selectByTimeRange(
            @Param("start") Date start,
            @Param("end") Date end
    );
}
