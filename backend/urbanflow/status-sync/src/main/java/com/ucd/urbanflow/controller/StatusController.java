package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.model.Edge;
import com.ucd.urbanflow.model.Junction;
import com.ucd.urbanflow.model.LaneEdgeInfo;
import com.ucd.urbanflow.model.TlsJunctionInfo;
import com.ucd.urbanflow.service.LaneService;
import com.ucd.urbanflow.service.RedisService;
import com.ucd.urbanflow.service.TlsJunctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api-status")
public class StatusController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private LaneService laneService;

    @Autowired
    private TlsJunctionService tlsJunctionService;


    @GetMapping("/edges")
    public ResponseEntity<Map<String, Edge>> getAllEdges() {
        Map<String, Edge> edgeMap = redisService.getAllEdgesFromHash();
        return ResponseEntity.ok(edgeMap);
    }

    @GetMapping("/junctions")
    public ResponseEntity<Map<String, Junction>> getAllJunctions() {
        Map<String, Junction> junctionMap = redisService.getAllJunctionsFromHash();
        return ResponseEntity.ok(junctionMap);
    }



    @GetMapping("/lane-mappings")
    public ResponseEntity<List<LaneEdgeInfo>> getLaneEdgeMappings() {
        List<LaneEdgeInfo> mappings = laneService.getLaneEdgeMappings();
        return ResponseEntity.ok(mappings);
    }

    @GetMapping("/tls-junctions")
    public ResponseEntity<List<TlsJunctionInfo>> getTlsJunctions() {
        return ResponseEntity.ok(tlsJunctionService.getAllTlsJunctions());
    }

}