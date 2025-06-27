package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.domain.dto.JunctionCongestionDTO;
import com.ucd.urbanflow.service.TrafficService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    @Autowired
    private TrafficService trafficService;

    @GetMapping("/congested-junctions")
    public ResponseEntity<List<JunctionCongestionDTO>> getCongestedJunctions() {
        List<JunctionCongestionDTO> congestedJunctions = trafficService.getCongestedJunctions();
        return ResponseEntity.ok(congestedJunctions);
    }
}