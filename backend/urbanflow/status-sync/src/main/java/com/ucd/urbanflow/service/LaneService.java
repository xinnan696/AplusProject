package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.LaneMapper;
import com.ucd.urbanflow.model.LaneEdgeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LaneService {

    @Autowired
    private LaneMapper laneMapper;

    public List<LaneEdgeInfo> getLaneEdgeMappings() {
        return laneMapper.getAllLaneEdgeMappings();
    }
}

