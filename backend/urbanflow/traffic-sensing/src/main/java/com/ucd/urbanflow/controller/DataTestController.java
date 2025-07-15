package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.domain.dto.EnrichedTrafficEvent;
import com.ucd.urbanflow.service.dataprocessing.RedisPollingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class DataTestController {

    private final RedisPollingService redisPollingService;

    @GetMapping("/emit")
    public String testEmit() {
        EnrichedTrafficEvent event = EnrichedTrafficEvent.builder()
                .edgeId("test-edge")
                .junctionId("test-junction")
                .junctionName("Test Junction")
                .simulationStep(System.currentTimeMillis())
                .vehicleCount(10)
                .waitTime(12.3)
                .waitingVehicleCount(3)
                .congested(false)
                .build();

        redisPollingService.getEventStream().subscribe(e -> {
            System.out.println("❗️Received event in stream: " + e);
        });

        redisPollingService.getSink().tryEmitNext(event); // 注意需要你对外暴露 getSink()
        return "Emitted test event";
    }
}