package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.JunctionIncomingEdge;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface JunctionMapper {

    /**
     * Retrieves all junctions and their associated incoming edge information.
     * @return A list of all junction-edge relationships.
     */
    List<JunctionIncomingEdge> findAllJunctionEdges();
}
