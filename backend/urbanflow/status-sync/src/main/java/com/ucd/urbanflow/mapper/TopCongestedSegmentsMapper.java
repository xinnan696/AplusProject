package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.model.TopCongestedSegments;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface TopCongestedSegmentsMapper {
    List<TopCongestedSegments> selectByTimeRange(
            @Param("start") Date start,
            @Param("end") Date end,
            @Param("junctionIds") List<String> junctionIds
    );
}
