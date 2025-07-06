package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.model.TrafficFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface TrafficFlowMapper {
    List<TrafficFlow> selectByJunctionAndTimeRange(
            @Param("junctionId") String junctionId,
            @Param("start") Date start,
            @Param("end") Date end
            );
}
