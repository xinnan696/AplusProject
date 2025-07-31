package com.ucd.urbanflow.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper for the new junction_regions table.
 */
@Mapper
public interface JunctionRegionsMapper {
    /**
     * Finds a list of junction IDs for a given area.
     * @param area The area to filter by (e.g., "Left" or "Right").
     * @return A list of junction IDs.
     */
    List<String> findJunctionIdsByArea(@Param("area") String area);
}