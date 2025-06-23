package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.JunctionIncomingEdge;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface JunctionMapper {

    /**
     * 查询所有路口及其对应的入口道路信息
     * @return A list of all junction-edge relationships.
     */
    List<JunctionIncomingEdge> findAllJunctionEdges();
}
