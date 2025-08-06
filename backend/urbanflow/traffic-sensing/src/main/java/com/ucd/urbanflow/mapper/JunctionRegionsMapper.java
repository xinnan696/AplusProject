package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.JunctionInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper for the new junction_regions table.
 */
@Mapper
public interface JunctionRegionsMapper {

    List<JunctionInfo> findJunctionsByArea(@Param("area") String area);
}