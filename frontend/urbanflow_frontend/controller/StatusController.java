package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.model.MapRealtimeData;
import com.ucd.urbanflow.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @Autowired
    private RedisService redisService;

    @GetMapping("/realtime")
    public ResponseEntity<MapRealtimeData> getRealtimeStatus() {
        MapRealtimeData data = redisService.getRealtimeData();
        return ResponseEntity.ok(data);
    }
}

