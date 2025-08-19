package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.TopCongestedSegments;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TopCongestedSegmentsMapper {

    void insert(TopCongestedSegments topCongestedSegments);
}
