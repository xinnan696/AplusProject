package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.dto.Junction;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface JunctionMapper {
    List<Junction> findAllJunctions();
}