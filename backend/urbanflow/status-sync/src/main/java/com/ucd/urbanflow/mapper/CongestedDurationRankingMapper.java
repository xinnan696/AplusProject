package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.model.CongestedDurationRanking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface CongestedDurationRankingMapper {
    List<CongestedDurationRanking> selectByTimeRange(
            @Param("start") Date start,
            @Param("end") Date end,
            @Param("junctionIds") List<String> junctionIds
    );
}
