package com.ucd.urbanflow.controller;


import com.ucd.urbanflow.model.JunctionStatus;
import com.ucd.urbanflow.service.AISuggestion;
import com.ucd.urbanflow.service.Redis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traffic")
public class TrafficSuggestionController {
    @Autowired
    private Redis redis;

    @Autowired
    private AISuggestion aiSuggestion;

    @PostMapping("/suggestion")
    public ResponseEntity<?> getSuggestion() {
        try {
            List<JunctionStatus> allStatus = redis.getAllJunctionStatus();

            if (allStatus == null || allStatus.isEmpty()){
                return ResponseEntity.status(404).body("NO DATA IN REDIS");
            }

            String aiResult = aiSuggestion.callFastApiBatch(allStatus);

            return ResponseEntity.ok(aiResult);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to get suggestions:" + e.getMessage());
        }
    }
}



