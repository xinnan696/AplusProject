package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.CongestedDurationRanking;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CongestedDurationRankingMapper {

    void insert(CongestedDurationRanking congestedDurationRanking);
}
