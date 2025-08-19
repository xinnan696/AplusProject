package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.service.TrafficFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class TrafficFlowController {
    @Autowired
    private TrafficFlowService trafficFlowService;

    @GetMapping("/trafficflow")
    public Map<String, Object> getDashboardData(
            @RequestParam String junctionId,
            @RequestParam String timeRange,
            @RequestParam(required = false) String managedAreas
    ) {
        return trafficFlowService.buildDashboardData(junctionId, timeRange, managedAreas);
    }
}